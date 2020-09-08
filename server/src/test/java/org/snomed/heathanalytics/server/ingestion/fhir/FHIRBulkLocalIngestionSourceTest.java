package org.snomed.heathanalytics.server.ingestion.fhir;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.snomed.heathanalytics.model.Patient;
import org.snomed.heathanalytics.server.TestConfig;
import org.snomed.heathanalytics.server.ingestion.elasticsearch.ElasticOutputStream;
import org.snomed.heathanalytics.server.store.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class FHIRBulkLocalIngestionSourceTest {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ElasticOutputStream elasticOutputStream;

	@Autowired
	private PatientRepository patientRepository;

	@Test
	public void testImport() {
		FHIRBulkLocalIngestionSourceConfiguration configuration = new FHIRBulkLocalIngestionSourceConfiguration(
				new File("src/test/resources/fhir/Patient.ndjson"), new File("src/test/resources/fhir/Condition.ndjson"));

		new FHIRBulkLocalIngestionSource(objectMapper).stream(configuration, elasticOutputStream);

		List<Patient> patients = patientRepository.findAll(Pageable.unpaged()).getContent();
		assertEquals(3, patients.size());
		for (Patient patient : patients) {
			System.out.println(patient.toString());
		}
		assertEquals("Patient{roleId='a850c94e-65d2-872c-1650-e52406d12ee5', gender=FEMALE, dob=1967-09-03, encounters=[ClinicalEncounter{conceptId='410429000', dateLong=404473115000}]}", patients.get(0).toString());
		assertEquals("Patient{roleId='83ae838f-9ab6-ca5c-778c-5b4054d79c57', gender=MALE, dob=1977-04-26, encounters=null}", patients.get(1).toString());
		assertEquals("Patient{roleId='a21e4c80-e45a-57c8-00bf-32788b395837', gender=MALE, dob=1995-04-17, encounters=null}", patients.get(2).toString());
	}

}
