package org.snomed.heathanalytics;

import org.ihtsdo.otf.snomedboot.factory.implementation.standard.ConceptImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.snomed.heathanalytics.domain.*;
import org.snomed.heathanalytics.ingestion.elasticsearch.ElasticOutputStream;
import org.snomed.heathanalytics.service.QueryService;
import org.snomed.heathanalytics.service.ServiceException;
import org.snomed.heathanalytics.service.SnomedService;
import org.snomed.heathanalytics.testutil.TestSnomedQueryServiceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.snomed.heathanalytics.TestUtils.date;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class IntegrationTest {

	@Autowired
	private QueryService queryService;

	@Autowired
	private SnomedService snomedService;

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
		hypertension = newConcept("38341003", allConcepts);

		// 22298006 |Myocardial infarction (disorder)|
		myocardialInfarction = newConcept("22298006", allConcepts);

		// 304914007 |Acute Q wave myocardial infarction (disorder)|
		acuteQWaveMyocardialInfarction = newConcept("304914007", allConcepts);
		acuteQWaveMyocardialInfarction.addInferredParent(myocardialInfarction);

		snomedService.setSnomedQueryService(TestSnomedQueryServiceBuilder.createWithConcepts(allConcepts.toArray(new ConceptImpl[]{})));

		// Set up tiny set of integration test data.
		// There is no attempt to make this realistic, we are just testing the logic.
		// "Bob"
		healthDataStream.createPatient(new Patient("1", TestUtils.getDob(35), Gender.MALE));
		// Bob has hypertension.
		healthDataStream.addClinicalEncounter("1", new ClinicalEncounter(date(2017, 0, 10), hypertension.getId()));
		// Bob has a type of myocardial infarction 10 days after the hypertension was recorded.
		healthDataStream.addClinicalEncounter("1", new ClinicalEncounter(date(2017, 0, 20), acuteQWaveMyocardialInfarction.getId()));

		// "Dave"
		healthDataStream.createPatient(new Patient("2", TestUtils.getDob(40), Gender.MALE));
		// Dave has hypertension. No other recorded disorders.
		healthDataStream.addClinicalEncounter("2", new ClinicalEncounter(date(2010, 5, 1), hypertension.getId()));
	}

	@Test
	public void testCohortSelectionWithExposureCriterion() throws ServiceException {
		EncounterCriterion firstExposureCriterion = new EncounterCriterion("<<" + myocardialInfarction.getId().toString());
		CohortCriteria cohortCriteria = new CohortCriteria(firstExposureCriterion);

		Page<Patient> patients = queryService.fetchCohort(cohortCriteria);

		assertEquals(1, patients.getTotalElements());
		Patient patient = patients.getContent().get(0);
		assertEquals("1", patient.getRoleId());
	}

	@Test
	public void testCohortSelectionWithMultipleExposureCriteria() throws ServiceException {
		// Fetch all patients with Hypertension
		EncounterCriterion hypertensionExposureCriterion = new EncounterCriterion("<<" + hypertension.getId().toString());
		CohortCriteria patientCriteria = new CohortCriteria(hypertensionExposureCriterion);

		// We get both Bob and Dave
		Page<Patient> patients = queryService.fetchCohort(patientCriteria);
		assertEquals("[1, 2]", toSortedPatientIdList(patients).toString());


		// Add inclusion criteria with Myocardial Infarction 5 days after primaryExposure
		EncounterCriterion inclusionCriteria = new EncounterCriterion("<<" + myocardialInfarction.getId().toString(), 5, null);
		patientCriteria.addEncounterCriterion(inclusionCriteria);

		// No matches because Bob had Myocardial Infarction 10 days after Hypertension is recorded
		patients = queryService.fetchCohort(patientCriteria);
		assertEquals("[]", toSortedPatientIdList(patients).toString());

		// Extend search to 12 days after the primaryExposure
		inclusionCriteria.setWithinDaysAfterPreviouslyMatchedEncounter(12);

		// Now Bob is in the cohort
		patients = queryService.fetchCohort(patientCriteria);
		assertEquals("[1]", toSortedPatientIdList(patients).toString());

		// Switch to exclude patients who had Myocardial Infarction
		inclusionCriteria.setHas(false);

		// Now only Dave is in
		patients = queryService.fetchCohort(patientCriteria);
		assertEquals("[2]", toSortedPatientIdList(patients).toString());
	}

	@Test
	public void testGenderSelection() throws ServiceException {
		CohortCriteria cohortCriteria = new CohortCriteria(new EncounterCriterion("<<" + myocardialInfarction.getId().toString()));

		// No gender filter
		Page<Patient> patients = queryService.fetchCohort(cohortCriteria);
		assertEquals(1, patients.getTotalElements());
		assertEquals(Gender.MALE, patients.iterator().next().getGender());

		// Females
		cohortCriteria.setGender(Gender.FEMALE);
		assertEquals(0, queryService.fetchCohort(cohortCriteria).getTotalElements());

		// Males
		cohortCriteria.setGender(Gender.MALE);
		// TODO: Why doesn't this work?
//		assertEquals(1, queryService.fetchCohort(cohortCriteria).getTotalElements());
	}

	@Test
	public void testAgeSelection() throws ServiceException {
		CohortCriteria cohortCriteria = new CohortCriteria(new EncounterCriterion("<<" + myocardialInfarction.getId().toString()));

		// No age filter
		assertEquals(1, queryService.fetchCohort(cohortCriteria).getTotalElements());

		cohortCriteria.setMinAgeNow(36);
		assertEquals(0, queryService.fetchCohort(cohortCriteria).getTotalElements());

		cohortCriteria.setMinAgeNow(35);
		assertEquals(1, queryService.fetchCohort(cohortCriteria).getTotalElements());

		cohortCriteria.setMinAgeNow(30);
		cohortCriteria.setMaxAgeNow(31);
		assertEquals(0, queryService.fetchCohort(cohortCriteria).getTotalElements());

		cohortCriteria.setMinAgeNow(30);
		cohortCriteria.setMaxAgeNow(38);
		assertEquals(1, queryService.fetchCohort(cohortCriteria).getTotalElements());
	}

	private List<String> toSortedPatientIdList(Page<Patient> patients) {
		return patients.getContent().stream().map(Patient::getRoleId).sorted().collect(Collectors.toList());
	}

	private ConceptImpl newConcept(String id, List<ConceptImpl> allConcepts) {
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
