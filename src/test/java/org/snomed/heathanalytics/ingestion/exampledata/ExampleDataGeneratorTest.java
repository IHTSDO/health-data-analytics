package org.snomed.heathanalytics.ingestion.exampledata;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.Sex;
import org.snomed.heathanalytics.ingestion.HealthDataOutputStream;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

public class ExampleDataGeneratorTest {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Test
	public void testStream() throws Exception {
		Map<String, List<String>> patientData = new HashMap<>();

		AtomicLong roleIdSource = new AtomicLong();
		new ExampleDataGenerator(new ExampleConceptService(), 20).stream(new HealthDataOutputStream() {
			@Override
			public String createPatient(String name, Date dateOfBirth, Sex sex) {
				String roleId = roleIdSource.incrementAndGet() + "";
				logger.info("New patient roleId:{}, name:{}, dob:{}, sex:{}", roleId, name, dateOfBirth, sex);
				synchronized (patientData) {
					patientData.put(roleId, new ArrayList<>());
				}
				return roleId;
			}

			@Override
			public void addCondition(String roleId, String conceptId) {
				logger.info("New condition roleId:{}, conceptId:{}", roleId, conceptId);
				patientData.get(roleId).add(conceptId);
			}
		});

		assertEquals(20, patientData.size());
	}

}