package org.snomed.heathanalytics;

import org.ihtsdo.otf.snomedboot.factory.implementation.standard.ConceptImpl;
import org.ihtsdo.otf.sqs.service.SnomedQueryService;
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
import org.snomed.heathanalytics.service.ServiceException;
import org.snomed.heathanalytics.testutil.TestSnomedQueryServiceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import static org.snomed.heathanalytics.TestUtils.date;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class IntegrationTest {

	@Autowired
	private QueryService queryService;

	@Autowired
	private ElasticOutputStream healthDataStream;

	private SnomedQueryService snomedQueryService;

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	private ConceptImpl myocardialInfarction;
	private ConceptImpl acuteQWaveMyocardialInfarction;

	@Before
	public void setup() throws IOException, ParseException {
		clearIndexes();

		// 22298006 |Myocardial infarction (disorder)|
		myocardialInfarction = createConcept("22298006");

		// 304914007 |Acute Q wave myocardial infarction (disorder)|
		acuteQWaveMyocardialInfarction = createConcept("304914007");
		acuteQWaveMyocardialInfarction.addInferredParent(myocardialInfarction);

		// Set up ECL query service test data
		snomedQueryService = TestSnomedQueryServiceBuilder.createWithConcepts(myocardialInfarction, acuteQWaveMyocardialInfarction);

		healthDataStream.createPatient("1", "Bob", date(1983), Sex.MALE);
		healthDataStream.addClinicalEncounter("1", date(2017, 0, 20), acuteQWaveMyocardialInfarction.getId());

		queryService.setSnomedQueryService(snomedQueryService);
	}

	@Test
	public void test() throws ServiceException {
		Page<Patient> patients = queryService.fetchCohort("<<" + myocardialInfarction.getId().toString());
		Assert.assertEquals(1, patients.getTotalElements());
		List<Patient> content = patients.getContent();
		Assert.assertEquals(1, content.size());
		Patient patient = content.get(0);
		Assert.assertEquals("1", patient.getRoleId());
		Assert.assertEquals(acuteQWaveMyocardialInfarction.getId(), patient.getEncounters().iterator().next().getConceptId());
	}

	private ConceptImpl createConcept(String id) {
		ConceptImpl concept = new ConceptImpl(id, "", true, "", "");
		concept.setFsn("");
		return concept;
	}

	@After
	public void clearIndexes() {
		elasticsearchTemplate.deleteIndex(Patient.class);
		elasticsearchTemplate.deleteIndex(ClinicalEncounter.class);
	}

}
