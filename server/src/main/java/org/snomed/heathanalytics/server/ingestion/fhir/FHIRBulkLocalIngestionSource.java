package org.snomed.heathanalytics.server.ingestion.fhir;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import org.elasticsearch.common.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.model.ClinicalEncounter;
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
		FHIRBulkLocalIngestionSourceConfiguration fhirConfiguration = (FHIRBulkLocalIngestionSourceConfiguration) configuration;
		ingestPatients(healthDataOutputStream, fhirConfiguration.getPatientFile());
		ingestConditions(healthDataOutputStream, fhirConfiguration.getConditionFile());
		ingestProcedures(healthDataOutputStream, fhirConfiguration.getProcedureFile());
	}

	private void ingestPatients(HealthDataOutputStream healthDataOutputStream, File patientFile) {
		logger.info("Reading Patients from {}.", patientFile.getPath());
		ObjectReader objectReader = objectMapper.readerFor(FHIRPatient.class);
		Date start = new Date();
		try {
			long read = 0;
			MappingIterator<FHIRPatient> patientIterator = objectReader.readValues(patientFile);
			for (UnmodifiableIterator<List<FHIRPatient>> it = Iterators.partition(patientIterator, ES_WRITE_BATCH_SIZE); it.hasNext(); ) {
				List<FHIRPatient> fhirPatients = it.next();
				List<Patient> patients = new ArrayList<>();
				for (FHIRPatient fhirPatient : fhirPatients) {
					patients.add(new Patient(fhirPatient.getId(), fhirPatient.getBirthDate(), Gender.from(fhirPatient.getGender())));
				}

				healthDataOutputStream.createPatients(patients);
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

	private void ingestConditions(HealthDataOutputStream healthDataOutputStream, File conditionFile) {
		logger.info("Reading Conditions from {}.", conditionFile.getPath());
		ObjectReader objectReader = objectMapper.readerFor(FHIRCondition.class);
		Date start = new Date();
		try {
			long active = 0;
			long all = 0;
			MappingIterator<FHIRCondition> conditionIterator = objectReader.readValues(conditionFile);
			while (conditionIterator.hasNext()) {
				FHIRCondition fhirCondition = conditionIterator.next();
				String subjectId = fhirCondition.getSubjectId();
				if (fhirCondition.isConfirmedActive() && subjectId != null) {
					String conceptId = getSnomedCode(fhirCondition.getCode());
					if (conceptId != null && fhirCondition.getOnsetDateTime() != null) {
						ClinicalEncounter encounter = new ClinicalEncounter(fhirCondition.getOnsetDateTime(), parseLong(conceptId));
						healthDataOutputStream.addClinicalEncounter(subjectId, encounter);
						active++;
						if (active % 10_000 == 0) {
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

	private void ingestProcedures(HealthDataOutputStream healthDataOutputStream, File procedureFile) {
		logger.info("Reading Procedures from {}.", procedureFile.getPath());
		ObjectReader objectReader = objectMapper.readerFor(FHIRProcedure.class);
		Date start = new Date();
		try {
			long active = 0;
			long all = 0;
			MappingIterator<FHIRProcedure> procedureIterator = objectReader.readValues(procedureFile);
			while (procedureIterator.hasNext()) {
				FHIRProcedure fhirProcedure = procedureIterator.next();
				String subjectId = fhirProcedure.getSubjectId();
				if (fhirProcedure.isComplete() && subjectId != null) {
					String conceptId = getSnomedCode(fhirProcedure.getCode());
					if (conceptId != null && fhirProcedure.getStartDate() != null) {
						ClinicalEncounter encounter = new ClinicalEncounter(fhirProcedure.getStartDate(), parseLong(conceptId));
						healthDataOutputStream.addClinicalEncounter(subjectId, encounter);
						active++;
						if (active % 10_000 == 0) {
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

	private String getSnomedCode(FHIRCodeableConcept codeableConcept) {
		if (codeableConcept != null) {
			List<FHIRCoding> coding = codeableConcept.getCoding();
			if (!coding.isEmpty()) {
				FHIRCoding fhirCoding = coding.get(0);
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
