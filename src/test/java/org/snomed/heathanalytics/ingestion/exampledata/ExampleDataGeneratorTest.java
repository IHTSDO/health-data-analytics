package org.snomed.heathanalytics.ingestion.exampledata;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.domain.Sex;
import org.snomed.heathanalytics.ingestion.HealthDataOutputStream;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

public class ExampleDataGeneratorTest {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Test
	public void testStream() throws Exception {
		Map<String, List<ClinicalEncounter>> patientData = new HashMap<>();

		new ExampleDataGenerator(new ExampleConceptService(), 20).stream(new HealthDataOutputStream() {
			@Override
			public void createPatient(String roleId, String name, Date dateOfBirth, Sex sex) {
				logger.info("New patient {}", new Patient(roleId, name, dateOfBirth, sex));
				synchronized (patientData) {
					patientData.put(roleId, new ArrayList<>());
				}
			}

			@Override
			public void addClinicalEncounter(String roleId, Date date, String conceptId) {
				ClinicalEncounter clinicalEncounter = new ClinicalEncounter(roleId, date, conceptId);
				logger.info("New clinical encounter {}", clinicalEncounter);
				patientData.get(clinicalEncounter.getRoleId()).add(clinicalEncounter);
			}
		});

		assertEquals(20, patientData.size());
	}

}