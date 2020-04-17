package org.snomed.heathanalytics.ingestion.exampledata;

import com.google.common.collect.Lists;
import org.ihtsdo.otf.sqs.service.ReleaseWriter;
import org.ihtsdo.otf.sqs.service.SnomedQueryService;
import org.ihtsdo.otf.sqs.service.dto.ConceptIdResults;
import org.ihtsdo.otf.sqs.service.exception.ServiceException;
import org.ihtsdo.otf.sqs.service.store.RamReleaseStore;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.ingestion.HealthDataOutputStream;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class ExampleDataGeneratorTest {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private ExampleDataGenerator exampleDataGenerator;

	@Before
	public void setup() throws IOException, ParseException {
		// Create tiny dataset for unit test only.
		SnomedQueryService snomedQueryService = new FakeQueryService();

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

	private static final class FakeQueryService extends SnomedQueryService {

		public FakeQueryService() throws IOException {
			super(getReleaseStore());
		}

		@Override
		// Respond to any query with the same dummy concept id
		public ConceptIdResults eclQueryReturnConceptIdentifiers(String ecQuery, int offset, int limit) throws ServiceException {
			return new ConceptIdResults(Lists.newArrayList(123L), 0, 1, 1);
		}

		// Initiate dummy ram release store, just to make Lucene happy
		private static RamReleaseStore getReleaseStore() throws IOException {
			RamReleaseStore ramReleaseStore = new RamReleaseStore();
			ReleaseWriter writer = new ReleaseWriter(ramReleaseStore);
			writer.close();
			return ramReleaseStore;
		}
	}
}
