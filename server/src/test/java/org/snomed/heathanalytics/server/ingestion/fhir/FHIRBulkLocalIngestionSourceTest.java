package org.snomed.heathanalytics.server.ingestion.fhir;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.snomed.heathanalytics.model.Patient;
import org.snomed.heathanalytics.server.AbstractDataTest;
import org.snomed.heathanalytics.server.ingestion.elasticsearch.ElasticOutputStream;
import org.snomed.heathanalytics.server.store.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FHIRBulkLocalIngestionSourceTest extends AbstractDataTest {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ElasticOutputStream elasticOutputStream;

	@Autowired
	private PatientRepository patientRepository;

	@Test
	public void testImport() {
		FHIRBulkLocalIngestionSourceConfiguration configuration = new FHIRBulkLocalIngestionSourceConfiguration(
				new File("src/test/resources/fhir/Patient.ndjson"),
				new File("src/test/resources/fhir/Condition.ndjson"),
				new File("src/test/resources/fhir/Procedure.ndjson"),
				new File("src/test/resources/fhir/MedicationRequest.ndjson"));

		new FHIRBulkLocalIngestionSource(objectMapper).stream(configuration, elasticOutputStream);

		List<Patient> patients = patientRepository.findAll(Pageable.unpaged()).getContent();
		assertEquals(3, patients.size());
		for (Patient patient : patients) {
			System.out.println(patient.toString());
		}

		assertEquals("Patient{roleId='a850c94e-65d2-872c-1650-e52406d12ee5', dataset=null, gender=FEMALE, dob=1967-09-03, numEncounters=2, encounters=[ClinicalEncounter{conceptId='117015009', dateLong=1474623515000}, ClinicalEncounter{conceptId='410429000', dateLong=404473115000}]}",
				getPatientString("a850c94e-65d2-872c-1650-e52406d12ee5"));

		assertEquals("Patient{roleId='83ae838f-9ab6-ca5c-778c-5b4054d79c57', dataset=null, gender=MALE, dob=1977-04-26, numEncounters=2, encounters=[ClinicalEncounter{conceptId='261352009', dateLong=1582537115000}, ClinicalEncounter{conceptId='430193006', dateLong=1310463515000}]}",
				getPatientString("83ae838f-9ab6-ca5c-778c-5b4054d79c57"));

		assertEquals("Patient{roleId='a21e4c80-e45a-57c8-00bf-32788b395837', dataset=null, gender=MALE, dob=1995-04-17, numEncounters=1, encounters=[ClinicalEncounter{conceptId='416897008', dateLong=-366615702000}]}",
				getPatientString("a21e4c80-e45a-57c8-00bf-32788b395837"));
	}

	@Test
	public void testOpenMRSFHIRImport() {
		FHIRBulkLocalIngestionSourceConfiguration configuration = new FHIRBulkLocalIngestionSourceConfiguration(
				new File("src/test/resources/fhir-open_mrs-export/Patient.ndjson"),
				new File("src/test/resources/fhir-open_mrs-export/Condition.ndjson"),
				null,
//				new File("src/test/resources/fhir-open_mrs-export/Procedure.ndjson"),
				new File("src/test/resources/fhir-open_mrs-export/MedicationRequest.ndjson"));

		new FHIRBulkLocalIngestionSource(objectMapper).stream(configuration, elasticOutputStream);

		List<Patient> patients = patientRepository.findAll(Pageable.unpaged()).getContent();
		assertEquals(1, patients.size());
		for (Patient patient : patients) {
			System.out.println(patient.toString());
		}

		assertEquals("Patient{roleId='b5201a4b-c8a9-4a2d-9fc3-08e2f6a3d8e0', dataset=null, gender=MALE, dob=2011-08-25, numEncounters=1, encounters=[ClinicalEncounter{conceptId='840539006', dateLong=1690277150000}]}",
				getPatientString("b5201a4b-c8a9-4a2d-9fc3-08e2f6a3d8e0"));
	}

	private String getPatientString(String id) {
		return patientRepository.findById(id).orElseGet(Patient::new).toString();
	}

}
