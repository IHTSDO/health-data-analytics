package org.snomed.heathanalytics.ingestion.exampledata;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import org.ihtsdo.otf.snomedboot.factory.implementation.standard.ConceptImpl;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.domain.Sex;
import org.snomed.heathanalytics.ingestion.HealthDataOutputStream;
import org.snomed.heathanalytics.snomed.SnomedSubsumptionService;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class ExampleDataGeneratorTest {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private ExampleDataGenerator exampleDataGenerator;

	@Before
	public void setup() {
		SnomedSubsumptionService snomedSubsumptionService = new SnomedSubsumptionService();
		Long2ObjectArrayMap<ConceptImpl> concepts = new Long2ObjectArrayMap<>();
		addTestConcept(concepts, "716020005", 420868002L);
		addTestConcept(concepts, "422426003", 302226006L);
		addTestConcept(concepts, "42531007", 22298006L);
		addTestConcept(concepts, "230645003", 302226006L);
		addTestConcept(concepts, "84094009", 38341003L);
		snomedSubsumptionService.setConcepts(concepts);
		exampleDataGenerator = new ExampleDataGenerator(new ExampleConceptService(snomedSubsumptionService), 20);
	}

	private ConceptImpl addTestConcept(Long2ObjectArrayMap<ConceptImpl> concepts, String conceptId, Long... descendent) {
		return concepts.put(Long.parseLong(conceptId), createTestConcept(conceptId, descendent));
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
			public void addClinicalEncounter(String roleId, Date date, String conceptId) {
				ClinicalEncounter clinicalEncounter = new ClinicalEncounter(roleId, date, conceptId);
				logger.info("New clinical encounter {}", clinicalEncounter);
				patientData.get(clinicalEncounter.getRoleId()).add(clinicalEncounter);
			}
		});

		assertEquals(20, patientData.size());
	}

	private ConceptImpl createTestConcept(final String conceptId, Long... ancestorIds) {
		return new ConceptImpl(conceptId) {
			@Override
			public Set<Long> getAncestorIds() throws IllegalStateException {
				return Sets.newHashSet(ancestorIds);
			}
		};
	}

}