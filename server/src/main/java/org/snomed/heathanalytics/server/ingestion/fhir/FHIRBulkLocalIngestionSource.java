package org.snomed.heathanalytics.server.ingestion.fhir;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import org.elasticsearch.common.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.model.ClinicalEvent;
import org.snomed.heathanalytics.model.Gender;
import org.snomed.heathanalytics.model.Patient;
import org.snomed.heathanalytics.server.ingestion.HealthDataIngestionSource;
import org.snomed.heathanalytics.server.ingestion.HealthDataIngestionSourceConfiguration;
import org.snomed.heathanalytics.server.ingestion.HealthDataOutputStream;
import org.snomed.heathanalytics.server.ingestion.SnomedIdentifierUtils;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.Long.parseLong;

public class FHIRBulkLocalIngestionSource implements HealthDataIngestionSource {

	private final ObjectMapper objectMapper;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public FHIRBulkLocalIngestionSource(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void stream(HealthDataIngestionSourceConfiguration configuration, HealthDataOutputStream healthDataOutputStream) {
		try (HealthDataOutputStream stream = healthDataOutputStream) {
			FHIRBulkLocalIngestionSourceConfiguration fhirConfiguration = (FHIRBulkLocalIngestionSourceConfiguration) configuration;
			ingestPatients(stream, fhirConfiguration.getPatientFile(), fhirConfiguration.getDataset());
			ingestConditions(stream, fhirConfiguration.getConditionFile(), fhirConfiguration.getDataset());
			ingestProcedures(stream, fhirConfiguration.getProcedureFile(), fhirConfiguration.getDataset());
			ingestMedicationRequests(stream, fhirConfiguration.getMedicationRequestFile(), fhirConfiguration.getDataset());
			ingestServiceRequests(stream, fhirConfiguration.getServiceRequestFile(), fhirConfiguration.getDataset());
		}
	}

	private void ingestPatients(HealthDataOutputStream healthDataOutputStream, File patientFile, String dataset) {
		if (patientFile == null) {
			logger.info("No Patients file supplied.");
			return;
		}
		logger.info("Reading Patients from {}.", patientFile.getPath());
		ObjectReader objectReader = objectMapper.readerFor(FHIRPatient.class);
		Date start = new Date();
		try (MappingIterator<FHIRPatient> patientIterator = objectReader.readValues(patientFile)) {
			long read = 0;
			for (UnmodifiableIterator<List<FHIRPatient>> it = Iterators.partition(patientIterator, ES_WRITE_BATCH_SIZE); it.hasNext(); ) {
				List<FHIRPatient> fhirPatients = it.next();
				List<Patient> patients = new ArrayList<>();
				for (FHIRPatient fhirPatient : fhirPatients) {
					patients.add(new Patient(fhirPatient.getId(), fhirPatient.getBirthDate(), Gender.from(fhirPatient.getGender())));
				}

				healthDataOutputStream.createPatients(patients, dataset);
				read += patients.size();
				if (read % 10_000 == 0) {
					logger.info("Consumed {} patients into store.", NumberFormat.getNumberInstance().format(read));
				}
			}
			logger.info("Read {} Patients from {} in {} seconds.", NumberFormat.getNumberInstance().format(read), patientFile.getPath(), (new Date().getTime() - start.getTime()) / 1_000);
		} catch (IOException e) {
			logger.error("Failed to read values from {}.", patientFile.getAbsolutePath(), e);
		}
	}

	private void ingestConditions(HealthDataOutputStream healthDataOutputStream, File conditionFile, String dataset) {
		if (conditionFile == null) {
			logger.info("No Conditions file supplied.");
			return;
		}
		logger.info("Reading Conditions from {}.", conditionFile.getPath());
		ObjectReader objectReader = objectMapper.readerFor(FHIRCondition.class);
		Date start = new Date();
		try (MappingIterator<FHIRCondition> conditionIterator = objectReader.readValues(conditionFile)) {
			long active = 0;
			long all = 0;
			while (conditionIterator.hasNext()) {
				FHIRCondition fhirCondition = conditionIterator.next();
				String subjectId = FHIRHelper.getSubjectId(fhirCondition.getSubject());
				if (fhirCondition.isConfirmedActive() && subjectId != null) {
					String conceptId = getSnomedCode(fhirCondition.getCode());
					if (conceptId != null && fhirCondition.getOnsetDateTime() != null) {
						ClinicalEvent event = new ClinicalEvent(fhirCondition.getOnsetDateTime(), parseLong(conceptId));
						healthDataOutputStream.addClinicalEvent(subjectId, event, dataset);
						active++;
						if (active % 1_000 == 0) {
							logger.info("Consumed {} Conditions into store.", NumberFormat.getNumberInstance().format(active));
						}
					}
				}
				all++;
			}

			logger.info("Consumed {} Conditions from {} in {} seconds. {} were inactive, not confirmed or not SNOMED CT codes so were discarded.",
					NumberFormat.getNumberInstance().format(active), conditionFile.getPath(), (new Date().getTime() - start.getTime()) / 1_000, all - active);
		} catch (IOException e) {
			logger.error("Failed to read values from {}.", conditionFile.getAbsolutePath(), e);
		}
	}

	private void ingestProcedures(HealthDataOutputStream healthDataOutputStream, File procedureFile, String dataset) {
		if (procedureFile == null) {
			logger.info("No Procedures file supplied.");
			return;
		}
		logger.info("Reading Procedures from {}.", procedureFile.getPath());
		ObjectReader objectReader = objectMapper.readerFor(FHIRProcedure.class);
		Date start = new Date();
		try (MappingIterator<FHIRProcedure> procedureIterator = objectReader.readValues(procedureFile)) {
			long active = 0;
			long all = 0;
			while (procedureIterator.hasNext()) {
				FHIRProcedure fhirProcedure = procedureIterator.next();
				String subjectId = FHIRHelper.getSubjectId(fhirProcedure.getSubject());
				if (fhirProcedure.isComplete() && subjectId != null) {
					String conceptId = getSnomedCode(fhirProcedure.getCode());
					if (conceptId != null && fhirProcedure.getStartDate() != null) {
						ClinicalEvent event = new ClinicalEvent(fhirProcedure.getStartDate(), parseLong(conceptId));
						healthDataOutputStream.addClinicalEvent(subjectId, event, dataset);
						active++;
						if (active % 1_000 == 0) {
							logger.info("Consumed {} Procedures into store.", NumberFormat.getNumberInstance().format(active));
						}
					}
				}
				all++;
			}

			logger.info("Consumed {} Procedures from {} in {} seconds. {} were inactive, not confirmed or not SNOMED CT codes so were discarded.",
					NumberFormat.getNumberInstance().format(active), procedureFile.getPath(), (new Date().getTime() - start.getTime()) / 1_000, all - active);
		} catch (IOException e) {
			logger.error("Failed to read values from {}.", procedureFile.getAbsolutePath(), e);
		}
	}

	private void ingestMedicationRequests(HealthDataOutputStream healthDataOutputStream, File medicationRequestFile, String dataset) {
		if (medicationRequestFile == null) {
			logger.info("No MedicationRequests file supplied.");
			return;
		}
		logger.info("Reading MedicationRequests from {}.", medicationRequestFile.getPath());
		ObjectReader objectReader = objectMapper.readerFor(FHIRMedicationRequest.class);
		Date start = new Date();
		try (MappingIterator<FHIRMedicationRequest> medicationRequestMappingIterator = objectReader.readValues(medicationRequestFile)) {
			long active = 0;
			long all = 0;
			while (medicationRequestMappingIterator.hasNext()) {
				FHIRMedicationRequest fhirMedicationRequest = medicationRequestMappingIterator.next();
				String subjectId = FHIRHelper.getSubjectId(fhirMedicationRequest.getSubject());
				if (fhirMedicationRequest.isActiveOrCompletedOrder() && subjectId != null) {
					String conceptId = getSnomedCode(fhirMedicationRequest.getMedicationCodeableConcept());
					if (conceptId != null && fhirMedicationRequest.getAuthoredOn() != null) {
						ClinicalEvent event = new ClinicalEvent(fhirMedicationRequest.getAuthoredOn(), parseLong(conceptId));
						healthDataOutputStream.addClinicalEvent(subjectId, event, dataset);
						active++;
						if (active % 1_000 == 0) {
							logger.info("Consumed {} Medications into store.", NumberFormat.getNumberInstance().format(active));
						}
					}
				}
				all++;
			}

			logger.info("Consumed {} Medications from {} in {} seconds. {} were inactive, not confirmed or not SNOMED CT codes so were discarded.",
					NumberFormat.getNumberInstance().format(active), medicationRequestFile.getPath(), (new Date().getTime() - start.getTime()) / 1_000, all - active);
		} catch (IOException e) {
			logger.error("Failed to read values from {}.", medicationRequestFile.getAbsolutePath(), e);
		}
	}

	private void ingestServiceRequests(HealthDataOutputStream healthDataOutputStream, File serviceRequestFile, String dataset) {
		if (serviceRequestFile == null) {
			logger.info("No ServiceRequests file supplied.");
			return;
		}
		logger.info("Reading ServiceRequests from {}.", serviceRequestFile.getPath());
		ObjectReader objectReader = objectMapper.readerFor(FHIRServiceRequest.class);
		Date start = new Date();
		try (MappingIterator<FHIRServiceRequest> serviceRequestIterator = objectReader.readValues(serviceRequestFile)) {
			long active = 0;
			long all = 0;
			while (serviceRequestIterator.hasNext()) {
				FHIRServiceRequest fhirServiceRequest = serviceRequestIterator.next();
				String subjectId = FHIRHelper.getSubjectId(fhirServiceRequest.getSubject());
				if (fhirServiceRequest.isCompleteOrLikelyComplete() && subjectId != null) {
					String conceptId = getSnomedCode(fhirServiceRequest.getCode());
					Date occurrenceDateOrBestGuess = fhirServiceRequest.getOccurrenceDateOrBestGuess();
					if (conceptId != null && occurrenceDateOrBestGuess != null) {
						ClinicalEvent event = new ClinicalEvent(occurrenceDateOrBestGuess, parseLong(conceptId));
						healthDataOutputStream.addClinicalEvent(subjectId, event, dataset);
						active++;
						if (active % 1_000 == 0) {
							logger.info("Consumed {} ServiceRequests into store.", NumberFormat.getNumberInstance().format(active));
						}
					}
				}
				all++;
			}

			logger.info("Consumed {} ServiceRequests from {} in {} seconds. {} were inactive, not confirmed or not SNOMED CT codes so were discarded.",
					NumberFormat.getNumberInstance().format(active), serviceRequestFile.getPath(), (new Date().getTime() - start.getTime()) / 1_000, all - active);
		} catch (IOException e) {
			logger.error("Failed to read values from {}.", serviceRequestFile.getAbsolutePath(), e);
		}
	}

	private String getSnomedCode(FHIRCodeableConcept codeableConcept) {
		if (codeableConcept != null) {
			List<FHIRCoding> coding = codeableConcept.getCoding();
			for (FHIRCoding fhirCoding : coding) {
				String system = fhirCoding.getSystem();
				if (system != null && system.startsWith("http://snomed.info/sct")) {
					String code = fhirCoding.getCode();
					if (!Strings.isNullOrEmpty(code) && SnomedIdentifierUtils.isValidConceptIdFormat(code)) {
						return code;
					}
				}
			}
		}
		return null;
	}

}
