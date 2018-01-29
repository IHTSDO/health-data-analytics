package org.snomed.heathanalytics.ingestion.exampledata;

import org.ihtsdo.otf.sqs.service.SnomedQueryService;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.domain.ClinicalEncounterType;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.ingestion.HealthDataOutputStream;
import org.snomed.heathanalytics.testutil.TestSnomedQueryServiceBuilder;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class ExampleDataGeneratorTest {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private ExampleDataGenerator exampleDataGenerator;

	@Before
	public void setup() throws IOException, ParseException {
		// Create tiny dataset for unit test only.
		SnomedQueryService snomedQueryService = TestSnomedQueryServiceBuilder.createWithConcepts(
				TestSnomedQueryServiceBuilder.concept("716020005", "420868002"),
				TestSnomedQueryServiceBuilder.concept("422426003", "302226006"),
				TestSnomedQueryServiceBuilder.concept("42531007", "22298006"),
				TestSnomedQueryServiceBuilder.concept("230645003", "302226006"),
				TestSnomedQueryServiceBuilder.concept("84094009", "38341003"),
				TestSnomedQueryServiceBuilder.concept("385682008", "108972005", "373873005", "138875005"),
				TestSnomedQueryServiceBuilder.concept("201791009", "69896004"),
				TestSnomedQueryServiceBuilder.concept("52661003", "69896004"),
				TestSnomedQueryServiceBuilder.concept("313296004", "13645005"),
				TestSnomedQueryServiceBuilder.concept("398728003", "416897008"),
				TestSnomedQueryServiceBuilder.concept("276693005", "53084003"),
				TestSnomedQueryServiceBuilder.concept("13200003", "404684003")
		);

		exampleDataGenerator = new ExampleDataGenerator(new ExampleConceptService(snomedQueryService));
	}

	@Test
	public void testStream() throws Exception {
		Map<String, Set<ClinicalEncounter>> patientData = new HashMap<>();

		exampleDataGenerator.stream(new ExampleDataGeneratorConfiguration(20), new HealthDataOutputStream() {
			@Override
			public void createPatient(Patient patient) {
				logger.info("New patient {}", patient);
				synchronized (patientData) {
					patientData.put(patient.getRoleId(), patient.getEncounters());
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
