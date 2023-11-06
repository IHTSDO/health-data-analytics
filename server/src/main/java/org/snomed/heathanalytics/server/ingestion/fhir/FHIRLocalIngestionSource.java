package org.snomed.heathanalytics.server.ingestion.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

import static java.lang.Long.parseLong;

public class FHIRLocalIngestionSource implements HealthDataIngestionSource {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void stream(HealthDataIngestionSourceConfiguration configuration, HealthDataOutputStream healthDataOutputStream) {
		try (HealthDataOutputStream stream = healthDataOutputStream) {
			FHIRLocalIngestionSourceConfiguration config = (FHIRLocalIngestionSourceConfiguration) configuration;
			File jsonDirectory = config.getFileDirectory();
			String dataset = config.getDataset();
			File[] files = jsonDirectory.listFiles((dir, name) -> name.endsWith(".json"));
			if (files != null) {
				FhirContext ctx;
				logger.info("Setting FHIR context to {}", config.getFhirVersion());
				switch (config.getFhirVersion()) {
					case dstu3: ctx = FhirContext.forDstu3();break;
					default   : ctx = FhirContext.forR4();
				}
				IParser parser = ctx.newJsonParser();
				List<org.snomed.heathanalytics.model.Patient> patients = new ArrayList<>();
				for (File jsonFile : files) {
					Date start = new Date();
					logger.info("Reading Patient from {}.", jsonFile.getPath());
					try {
						org.snomed.heathanalytics.model.Patient elPatient;
						switch (config.getFhirVersion()) {
							case dstu3: elPatient = parse_dstu3(jsonFile, parser);break;
							default   : elPatient = parse_r4(jsonFile, parser);
						}
						if (elPatient != null)
							patients.add(elPatient);
						if (patients.size() == 1000) {
							stream.createPatients(patients, dataset);
							patients.clear();
						}
					} catch (Exception e) {
						logger.error("Failed to read values from {}.", jsonFile.getAbsolutePath(), e);
					}
				}
				if (!patients.isEmpty()) {
					stream.createPatients(patients, dataset);
				}
			}
		}
	}

	private Patient parse_r4(File jsonFile, IParser parser) throws FileNotFoundException {
		org.snomed.heathanalytics.model.Patient elPatient = null;
		org.hl7.fhir.r4.model.Bundle bundle = parser.parseResource(org.hl7.fhir.r4.model.Bundle.class, new FileInputStream(jsonFile));
		Optional<org.hl7.fhir.r4.model.Bundle.BundleEntryComponent> patientResource =
				bundle.getEntry().stream().filter(
						(entry) -> entry.getResource().fhirType().equals("Patient")
				).findFirst();
		if (patientResource.isPresent()) {
			org.hl7.fhir.r4.model.Patient patient = (org.hl7.fhir.r4.model.Patient) patientResource.get().getResource();
			elPatient = new org.snomed.heathanalytics.model.Patient(
					patient.getId(),
					patient.getBirthDate(),
					Gender.from(patient.getGender().getDisplay())
			);
			Patient finalElPatient = elPatient;
			bundle.getEntry().stream().forEach(bundleEntryComponent -> {
				if (bundleEntryComponent.getResource().fhirType().equals("Condition")) {
					org.hl7.fhir.r4.model.Condition condition = (org.hl7.fhir.r4.model.Condition) bundleEntryComponent.getResource();
					if (isConfirmedActive(condition)) {
						String conceptId = getSCTCode(condition.getCode());
						if ((conceptId != null) && (condition.getRecordedDate() != null))
							finalElPatient.addEvent(new ClinicalEvent(condition.getRecordedDate(), parseLong(conceptId)));
					}
				}
				if (bundleEntryComponent.getResource().fhirType().equals("Procedure")) {
					org.hl7.fhir.r4.model.Procedure procedure = (org.hl7.fhir.r4.model.Procedure) bundleEntryComponent.getResource();
					String conceptId = getSCTCode(procedure.getCode());
					Date performedOn = getProcedureDate(procedure);
					if ((conceptId != null) && (performedOn != null))
						finalElPatient.addEvent(new ClinicalEvent(performedOn, parseLong(conceptId)));
				}
				if (bundleEntryComponent.getResource().fhirType().equals("MedicationRequest")) {
					org.hl7.fhir.r4.model.MedicationRequest medicationRequest = (org.hl7.fhir.r4.model.MedicationRequest) bundleEntryComponent.getResource();
					String conceptId = getSCTCode(medicationRequest.getMedicationCodeableConcept());
					if ((conceptId != null) && (medicationRequest.getAuthoredOn() != null))
						finalElPatient.addEvent(new ClinicalEvent(medicationRequest.getAuthoredOn(), parseLong(conceptId)));
				}
			});
		}
		return elPatient;
	}

	private Patient parse_dstu3(File jsonFile, IParser parser) throws FileNotFoundException {
		org.snomed.heathanalytics.model.Patient elPatient = null;
		org.hl7.fhir.dstu3.model.Bundle bundle = parser.parseResource(org.hl7.fhir.dstu3.model.Bundle.class, new FileInputStream(jsonFile));
		Optional<org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent> patientResource =
				bundle.getEntry().stream().filter(
						(entry) -> entry.getResource().fhirType().equals("Patient")
				).findFirst();
		if (patientResource.isPresent()) {
			org.hl7.fhir.dstu3.model.Patient patient = (org.hl7.fhir.dstu3.model.Patient) patientResource.get().getResource();
			elPatient = new org.snomed.heathanalytics.model.Patient(
					patient.getId(),
					patient.getBirthDate(),
					Gender.from(patient.getGender().getDisplay())
			);
			Patient finalElPatient = elPatient;
			bundle.getEntry().stream().forEach(bundleEntryComponent -> {
				if (bundleEntryComponent.getResource().fhirType().equals("Condition")) {
					org.hl7.fhir.dstu3.model.Condition condition = (org.hl7.fhir.dstu3.model.Condition) bundleEntryComponent.getResource();
					if (isConfirmedActive(condition)) {
						String conceptId = getSCTCode(condition.getCode());
						if ((conceptId != null) && (condition.getAssertedDate() != null))
							finalElPatient.addEvent(new ClinicalEvent(condition.getAssertedDate(), parseLong(conceptId)));
					}
				}
				if (bundleEntryComponent.getResource().fhirType().equals("Procedure")) {
					org.hl7.fhir.dstu3.model.Procedure procedure = (org.hl7.fhir.dstu3.model.Procedure) bundleEntryComponent.getResource();
					String conceptId = getSCTCode(procedure.getCode());
					Date performedOn = getProcedureDate(procedure);
					if ((conceptId != null) && (performedOn != null))
						finalElPatient.addEvent(new ClinicalEvent(performedOn, parseLong(conceptId)));
				}
			});
		}
		return elPatient;
	}

	private Date getProcedureDate(org.hl7.fhir.r4.model.Procedure procedure) {
		if (procedure.getPerformed().fhirType().equals("dateTime"))
			return procedure.getPerformedDateTimeType().getValue();
		if (procedure.getPerformed().fhirType().equals("Period"))
			return procedure.getPerformedPeriod().getStart();
		return null;
	}

	private Date getProcedureDate(org.hl7.fhir.dstu3.model.Procedure procedure) {
		if (procedure.getPerformed().fhirType().equals("dateTime"))
			return procedure.getPerformedDateTimeType().getValue();
		if (procedure.getPerformed().fhirType().equals("Period"))
			return procedure.getPerformedPeriod().getStart();
		return null;
	}

	private String getSCTCode(org.hl7.fhir.r4.model.CodeableConcept codeableConcept) {
		if (codeableConcept != null) {
			List<org.hl7.fhir.r4.model.Coding> coding = codeableConcept.getCoding();
			if (!coding.isEmpty()) {
				org.hl7.fhir.r4.model.Coding fhirCoding = coding.get(0);
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

	private String getSCTCode(org.hl7.fhir.dstu3.model.CodeableConcept codeableConcept) {
		if (codeableConcept != null) {
			List<org.hl7.fhir.dstu3.model.Coding> coding = codeableConcept.getCoding();
			if (!coding.isEmpty()) {
				org.hl7.fhir.dstu3.model.Coding fhirCoding = coding.get(0);
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

	protected boolean isConfirmedActive(org.hl7.fhir.r4.model.Condition condition) {
		if (condition.getClinicalStatus() != null) {
			if (!condition.getClinicalStatus().getCoding().isEmpty()) {
				org.hl7.fhir.r4.model.Coding status = condition.getClinicalStatus().getCoding().iterator().next();
				if ("http://terminology.hl7.org/CodeSystem/condition-clinical".equals(status.getSystem())) {
					String code = status.getCode();
					if (code != null) {
						if (!code.equals("active") && !code.equals("recurrence") && !code.equals("relapse")) {
							return false;
						}
					}
				}
			}
		}
		if (condition.getVerificationStatus() != null) {
			if (!condition.getVerificationStatus().getCoding().isEmpty()) {
				org.hl7.fhir.r4.model.Coding status = condition.getVerificationStatus().getCoding().iterator().next();
				if ("http://terminology.hl7.org/CodeSystem/condition-ver-status".equals(status.getSystem())) {
					String code = status.getCode();
					if (code != null) {
						if (!code.equals("confirmed")) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	protected boolean isConfirmedActive(org.hl7.fhir.dstu3.model.Condition condition) {
		if (condition.getClinicalStatus() != null) {
			if (condition.getClinicalStatus() != null) {
				if ("http://terminology.hl7.org/CodeSystem/condition-clinical".equals(condition.getClinicalStatus().getSystem())) {
					String code = condition.getClinicalStatus().toCode();
					if (code != null) {
						if (!code.equals("active") && !code.equals("recurrence") && !code.equals("relapse")) {
							return false;
						}
					}
				}
			}
		}
		if (condition.getVerificationStatus() != null) {
			if (condition.getVerificationStatus() != null) {
				if ("http://terminology.hl7.org/CodeSystem/condition-ver-status".equals(condition.getVerificationStatus().getSystem())) {
					String code = condition.getVerificationStatus().toCode();
					if (code != null) {
						if (!code.equals("confirmed")) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

}
