package org.snomed.heathanalytics;

import org.ihtsdo.otf.snomedboot.factory.implementation.standard.ConceptImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.snomed.heathanalytics.domain.*;
import org.snomed.heathanalytics.ingestion.elasticsearch.ElasticOutputStream;
import org.snomed.heathanalytics.service.Criterion;
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
		healthDataStream.createPatient("1", "Bob", TestUtils.getDob(35), Gender.MALE);
		healthDataStream.addClinicalEncounter("1", date(2017, 0, 20), ClinicalEncounterType.FINDING, acuteQWaveMyocardialInfarction.getId());

		queryService.setSnomedQueryService(TestSnomedQueryServiceBuilder.createWithConcepts(myocardialInfarction, acuteQWaveMyocardialInfarction));
	}

	@Test
	public void test() throws ServiceException {
		Page<Patient> patients = queryService.fetchCohort(new CohortCriteria(new Criterion("<<" + myocardialInfarction.getId().toString())));
		Assert.assertEquals(1, patients.getTotalElements());
		List<Patient> content = patients.getContent();
		Assert.assertEquals(1, content.size());
		Patient patient = content.get(0);
		Assert.assertEquals("1", patient.getRoleId());
		Assert.assertEquals(acuteQWaveMyocardialInfarction.getId(), patient.getEncounters().iterator().next().getConceptId());
	}

	@Test
	public void testGenderSelection() throws ServiceException {
		CohortCriteria cohortCriteria = new CohortCriteria(new Criterion("<<" + myocardialInfarction.getId().toString()));

		// No gender filter
		Assert.assertEquals(1, queryService.fetchCohort(cohortCriteria).getTotalElements());

		// Females
		cohortCriteria.setGender(Gender.FEMALE);
		Assert.assertEquals(0, queryService.fetchCohort(cohortCriteria).getTotalElements());

		// Males
		cohortCriteria.setGender(Gender.MALE);
		Assert.assertEquals(1, queryService.fetchCohort(cohortCriteria).getTotalElements());
	}

	@Test
	public void testAgeSelection() throws ServiceException {
		CohortCriteria cohortCriteria = new CohortCriteria(new Criterion("<<" + myocardialInfarction.getId().toString()));

		// No age filter
		Assert.assertEquals(1, queryService.fetchCohort(cohortCriteria).getTotalElements());

		cohortCriteria.setMinAge(36);
		Assert.assertEquals(0, queryService.fetchCohort(cohortCriteria).getTotalElements());

		cohortCriteria.setMinAge(35);
		Assert.assertEquals(1, queryService.fetchCohort(cohortCriteria).getTotalElements());

		cohortCriteria.setMinAge(30);
		cohortCriteria.setMaxAge(31);
		Assert.assertEquals(0, queryService.fetchCohort(cohortCriteria).getTotalElements());

		cohortCriteria.setMinAge(30);
		cohortCriteria.setMaxAge(38);
		Assert.assertEquals(1, queryService.fetchCohort(cohortCriteria).getTotalElements());
	}

	private ConceptImpl createConcept(String id) {
		ConceptImpl concept = new ConceptImpl(id, "20170731", true, "", "");
		concept.setFsn("");
		return concept;
	}

	@After
	public void clearIndexes() {
		elasticsearchTemplate.deleteIndex(Patient.class);
		elasticsearchTemplate.deleteIndex(ClinicalEncounter.class);
	}

}
