package org.snomed.heathanalytics.ingestion.exampledata;

import org.ihtsdo.otf.sqs.service.SnomedQueryService;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.domain.Sex;
import org.snomed.heathanalytics.ingestion.HealthDataOutputStream;
import org.snomed.heathanalytics.testutil.TestSnomedQueryServiceBuilder;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class ExampleDataGeneratorTest {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private ExampleDataGenerator exampleDataGenerator;

	@Before
	public void setup() throws IOException {
		SnomedQueryService snomedQueryService = TestSnomedQueryServiceBuilder.createWithConcepts(
				TestSnomedQueryServiceBuilder.concept("716020005", "420868002"),
				TestSnomedQueryServiceBuilder.concept("422426003", "302226006"),
				TestSnomedQueryServiceBuilder.concept("42531007", "22298006"),
				TestSnomedQueryServiceBuilder.concept("230645003", "302226006"),
				TestSnomedQueryServiceBuilder.concept("84094009", "38341003")
		);

		exampleDataGenerator = new ExampleDataGenerator(new ExampleConceptService(snomedQueryService), 20);
	}

	@Test
	public void testStream() throws Exception {
		Map<String, List<ClinicalEncounter>> patientData = new HashMap<>();

		exampleDataGenerator.stream(new HealthDataOutputStream() {
			@Override
			public void createPatient(String roleId, String name, Date dateOfBirth, Sex sex) {
				logger.info("New patient {}", new Patient(roleId, name, dateOfBirth, sex));
				synchronized (patientData) {
					patientData.put(roleId, new ArrayList<>());
				}
			}

			@Override
			public void addClinicalEncounter(String roleId, Date date, Long conceptId) {
				ClinicalEncounter clinicalEncounter = new ClinicalEncounter(roleId, date, conceptId);
				logger.info("New clinical encounter {}", clinicalEncounter);
				patientData.get(clinicalEncounter.getRoleId()).add(clinicalEncounter);
			}
		});

		assertEquals(20, patientData.size());
	}

}
