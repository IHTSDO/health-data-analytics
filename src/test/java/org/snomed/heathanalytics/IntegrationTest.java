package org.snomed.heathanalytics;

import org.ihtsdo.otf.snomedboot.factory.implementation.standard.ConceptImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.snomed.heathanalytics.domain.*;
import org.snomed.heathanalytics.ingestion.elasticsearch.ElasticOutputStream;
import org.snomed.heathanalytics.domain.Criterion;
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
import java.util.*;
import java.util.stream.Collectors;

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

	private ConceptImpl hypertension;
	private ConceptImpl myocardialInfarction;
	private ConceptImpl acuteQWaveMyocardialInfarction;

	@Before
	public void setup() throws IOException, ParseException {
		clearIndexes();

		// Set up ECL query service test data
		List<ConceptImpl> allConcepts = new ArrayList<>();

		// 38341003 |Hypertensive disorder, systemic arterial (disorder)|
		hypertension = createConcept("38341003", allConcepts);

		// 22298006 |Myocardial infarction (disorder)|
		myocardialInfarction = createConcept("22298006", allConcepts);

		// 304914007 |Acute Q wave myocardial infarction (disorder)|
		acuteQWaveMyocardialInfarction = createConcept("304914007", allConcepts);
		acuteQWaveMyocardialInfarction.addInferredParent(myocardialInfarction);

		queryService.setSnomedQueryService(TestSnomedQueryServiceBuilder.createWithConcepts(allConcepts.toArray(new ConceptImpl[]{})));

		// Set up tiny set of integration test data.
		// There is no attempt to make this realistic, we are just testing the logic.
		healthDataStream.createPatient(new Patient("1", "Bob", TestUtils.getDob(35), Gender.MALE));
		healthDataStream.addClinicalEncounter("1", new ClinicalEncounter(date(2017, 0, 10), ClinicalEncounterType.FINDING, hypertension.getId()));
		healthDataStream.addClinicalEncounter("1", new ClinicalEncounter(date(2017, 0, 20), ClinicalEncounterType.FINDING, acuteQWaveMyocardialInfarction.getId()));

		healthDataStream.createPatient(new Patient("2", "Dave", TestUtils.getDob(40), Gender.MALE));
		healthDataStream.addClinicalEncounter("2", new ClinicalEncounter(date(2010, 5, 1), ClinicalEncounterType.FINDING, hypertension.getId()));
	}

	@Test
	public void testCohortSelectionWithPrimaryExposureCriterion() throws ServiceException {
		Criterion primaryExposureCriterion = new Criterion("<<" + myocardialInfarction.getId().toString());
		CohortCriteria cohortCriteria = new CohortCriteria(primaryExposureCriterion);

		Page<Patient> patients = queryService.fetchCohort(cohortCriteria);

		Assert.assertEquals(1, patients.getTotalElements());
		Patient patient = patients.getContent().get(0);
		Assert.assertEquals("1", patient.getRoleId());
		Assert.assertEquals(acuteQWaveMyocardialInfarction.getId(), patient.getEncounters().stream().filter(ClinicalEncounter::isPrimaryExposure).iterator().next().getConceptId());
	}

	@Test
	public void testCohortSelectionWithPrimaryExposureAndInclusionCriteria() throws ServiceException {
		// Fetch all patients with Hypertension
		Criterion primaryExposureCriterion = new Criterion("<<" + hypertension.getId().toString());
		CohortCriteria cohortCriteria = new CohortCriteria(primaryExposureCriterion);

		// We get both Bob and Dave
		Page<Patient> patients = queryService.fetchCohort(cohortCriteria);
		Assert.assertEquals("[1, 2]", toSortedPatientIdList(patients).toString());


		// Add inclusion criteria with Myocardial Infarction 5 days after primaryExposure
		RelativeCriterion inclusionCriteria = new RelativeCriterion("<<" + myocardialInfarction.getId().toString(), null, 5);
		cohortCriteria.addAdditionalCriterion(inclusionCriteria);

		// No matches because Bob had Myocardial Infarction 10 days after Hypertension is recorded
		patients = queryService.fetchCohort(cohortCriteria);
		Assert.assertEquals("[]", toSortedPatientIdList(patients).toString());

		// Extend search to 12 days after the primaryExposure
		inclusionCriteria.setIncludeDaysInFuture(12);

		// Now Bob is in the cohort
		patients = queryService.fetchCohort(cohortCriteria);
		Assert.assertEquals("[1]", toSortedPatientIdList(patients).toString());

		// Switch to exclude patients who had Myocardial Infarction
		inclusionCriteria.setHas(false);

		// Now only Dave is in
		patients = queryService.fetchCohort(cohortCriteria);
		Assert.assertEquals("[2]", toSortedPatientIdList(patients).toString());
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

	private List<String> toSortedPatientIdList(Page<Patient> patients) {
		List<String> list = patients.getContent().stream().map(Patient::getRoleId).collect(Collectors.toList());
		Collections.sort(list);
		return list;
	}

	private ConceptImpl createConcept(String id, List<ConceptImpl> allConcepts) {
		ConceptImpl concept = new ConceptImpl(id, "20170731", true, "", "");
		concept.setFsn("");
		allConcepts.add(concept);
		return concept;
	}

	@After
	public void clearIndexes() {
		elasticsearchTemplate.deleteIndex(Patient.class);
	}

}
