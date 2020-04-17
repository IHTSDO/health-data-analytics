package org.snomed.heathanalytics.ingestion.exampledata;

import org.ihtsdo.otf.sqs.service.exception.ServiceException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.ingestion.HealthDataOutputStream;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExampleDataGeneratorTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private ExampleDataGenerator exampleDataGenerator;

	@Before
	public void setup() throws ServiceException {
		ExampleConceptService mockConceptService = mock(ExampleConceptService.class);
		when(mockConceptService.selectRandomChildOf(anyString())).thenReturn(123L);
		exampleDataGenerator = new ExampleDataGenerator(mockConceptService);
	}

	@Test
	public void testStream() {
		Map<String, Set<ClinicalEncounter>> patientData = new HashMap<>();

		int demoPatientCount = 20;
		exampleDataGenerator.stream(new ExampleDataGeneratorConfiguration(demoPatientCount), new HealthDataOutputStream() {
			@Override
			public void createPatient(Patient patient) {
				logger.info("New patient {}", patient);
				synchronized (patientData) {
					patientData.put(patient.getRoleId(), patient.getEncounters());
				}
			}

			@Override
			public void createPatients(Collection<Patient> patients) {
				logger.info("New patient batch of {}", patients.size());
				synchronized (patientData) {
					for (Patient patient : patients) {
						patientData.put(patient.getRoleId(), patient.getEncounters());
					}
				}
			}

			@Override
			public void addClinicalEncounter(String roleId, ClinicalEncounter encounter) {
				logger.info("New clinical encounter {} {}", roleId, encounter);
				patientData.get(roleId).add(encounter);
			}
		});

		assertEquals(20, patientData.size());
	}

}
