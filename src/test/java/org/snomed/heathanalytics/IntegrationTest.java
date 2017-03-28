package org.snomed.heathanalytics;

import org.ihtsdo.otf.snomedboot.factory.implementation.standard.ConceptImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.domain.Sex;
import org.snomed.heathanalytics.ingestion.elasticsearch.ElasticOutputStream;
import org.snomed.heathanalytics.service.QueryService;
import org.snomed.heathanalytics.snomed.SnomedSubsumptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.snomed.heathanalytics.TestUtils.date;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class IntegrationTest {

	@Autowired
	private QueryService queryService;

	@Autowired
	private ElasticOutputStream healthDataStream;

	@Autowired
	private SnomedSubsumptionService snomedSubsumptionService;

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	private ConceptImpl myocardialInfarction;
	private ConceptImpl acuteQWaveMyocardialInfarction;

	@Before
	public void setup() {
		clearIndexes();

		Set<ConceptImpl> concepts = new HashSet<>();

		// 22298006 |Myocardial infarction (disorder)|
		myocardialInfarction = addConcept("22298006", concepts);

		// 304914007 |Acute Q wave myocardial infarction (disorder)|
		acuteQWaveMyocardialInfarction = addConcept("304914007", concepts);
		acuteQWaveMyocardialInfarction.addInferredParent(myocardialInfarction);

		snomedSubsumptionService.setConcepts(concepts);

		healthDataStream.createPatient("1", "Bob", date(1983), Sex.MALE);
		healthDataStream.addClinicalEncounter("1", date(2017, 0, 20), acuteQWaveMyocardialInfarction.getId().toString());
	}

	@Test
	public void test() {
		Page<ClinicalEncounter> clinicalEncounters = queryService.fetchCohort(myocardialInfarction.getId().toString());
		Assert.assertEquals(1, clinicalEncounters.getTotalElements());
		List<ClinicalEncounter> content = clinicalEncounters.getContent();
		Assert.assertEquals(1, content.size());
		ClinicalEncounter clinicalEncounter = content.get(0);
		Assert.assertEquals("1", clinicalEncounter.getRoleId());
		Assert.assertEquals(acuteQWaveMyocardialInfarction.getId().toString(), clinicalEncounter.getConceptId());
	}

	private ConceptImpl addConcept(String id, Set<ConceptImpl> concepts) {
		ConceptImpl concept = new ConceptImpl(id, "", true, "", "");
		concepts.add(concept);
		return concept;
	}

	@After
	public void clearIndexes() {
		elasticsearchTemplate.deleteIndex(Patient.class);
		elasticsearchTemplate.deleteIndex(ClinicalEncounter.class);
	}

}
