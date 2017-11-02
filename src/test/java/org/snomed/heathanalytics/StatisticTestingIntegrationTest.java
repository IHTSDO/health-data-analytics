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
import org.snomed.heathanalytics.service.StatisticalTestResult;
import org.snomed.heathanalytics.testutil.TestSnomedQueryServiceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.snomed.heathanalytics.TestUtils.date;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class StatisticTestingIntegrationTest {

	@Autowired
	private QueryService queryService;

	@Autowired
	private ElasticOutputStream healthDataStream;

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	private ConceptImpl hypertension;
	private ConceptImpl myocardialInfarction;
	private ConceptImpl acuteQWaveMyocardialInfarction;
	private ConceptImpl paracetamol;

	private int year = 365;

	@Before
	public void setup() throws IOException, ParseException {
		clearIndexes();

		// Set up ECL query service test data
		List<ConceptImpl> allConcepts = new ArrayList<>();

		// 38341003 |Hypertensive disorder, systemic arterial (disorder)|
		hypertension = createConcept("38341003", allConcepts);

		// 22298006 |Myocardial infarction (disorder)|
		// Also known as "heart attack"
		myocardialInfarction = createConcept("22298006", allConcepts);

		// 304914007 |Acute Q wave myocardial infarction (disorder)|
		acuteQWaveMyocardialInfarction = createConcept("304914007", allConcepts);
		acuteQWaveMyocardialInfarction.addInferredParent(myocardialInfarction);

		// Todo: Get code for paracetamol
		// 51234001 |Paracetamol|
		paracetamol = createConcept("51234001", allConcepts);

		queryService.setSnomedQueryService(TestSnomedQueryServiceBuilder.createWithConcepts(allConcepts.toArray(new ConceptImpl[]{})));

		//
		// Set up some data for this test
		//

		// 1 patient without hypertension and with heart attack
		Patient patient = createPatient("1");
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2015, 2, 1), ClinicalEncounterType.FINDING, acuteQWaveMyocardialInfarction.getId()));

		// 2 patients with hypertension and no Paracetamol and no heart attack
		patient = createPatient("2");
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2017, 0, 10), ClinicalEncounterType.FINDING, hypertension.getId()));
		patient = createPatient("3");
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2017, 0, 15), ClinicalEncounterType.FINDING, hypertension.getId()));

		// 4 patients with hypertension and no Paracetamol and subsequent heart attack
		patient = createPatient("4");
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2015, 0, 1), ClinicalEncounterType.FINDING, hypertension.getId()));
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2017, 0, 1), ClinicalEncounterType.FINDING, acuteQWaveMyocardialInfarction.getId()));
		patient = createPatient("5");
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2016, 0, 1), ClinicalEncounterType.FINDING, hypertension.getId()));
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2017, 0, 1), ClinicalEncounterType.FINDING, acuteQWaveMyocardialInfarction.getId()));
		patient = createPatient("6");
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2015, 5, 1), ClinicalEncounterType.FINDING, hypertension.getId()));
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2017, 2, 1), ClinicalEncounterType.FINDING, acuteQWaveMyocardialInfarction.getId()));
		patient = createPatient("7");
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2015, 1, 1), ClinicalEncounterType.FINDING, hypertension.getId()));
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2017, 1, 1), ClinicalEncounterType.FINDING, acuteQWaveMyocardialInfarction.getId()));

		// 3 patients with hypertension and Paracetamol and no heart attack
		patient = createPatient("8");
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2017, 0, 10), ClinicalEncounterType.FINDING, hypertension.getId()));
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2017, 3, 10), ClinicalEncounterType.FINDING, paracetamol.getId()));
		patient = createPatient("9");
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2017, 0, 15), ClinicalEncounterType.FINDING, hypertension.getId()));
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2017, 3, 10), ClinicalEncounterType.FINDING, paracetamol.getId()));
		patient = createPatient("10");
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2015, 0, 1), ClinicalEncounterType.FINDING, hypertension.getId()));
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2017, 3, 10), ClinicalEncounterType.FINDING, paracetamol.getId()));

		// 2 patients with hypertension and Paracetamol and subsequent heart attack
		patient = createPatient("11");
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2016, 0, 1), ClinicalEncounterType.FINDING, hypertension.getId()));
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2016, 3, 10), ClinicalEncounterType.FINDING, paracetamol.getId()));
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2017, 0, 1), ClinicalEncounterType.FINDING, acuteQWaveMyocardialInfarction.getId()));
		patient = createPatient("12");
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2016, 0, 1), ClinicalEncounterType.FINDING, hypertension.getId()));
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2016, 3, 10), ClinicalEncounterType.FINDING, paracetamol.getId()));
		healthDataStream.addClinicalEncounter(patient.getRoleId(), new ClinicalEncounter(date(2017, 2, 1), ClinicalEncounterType.FINDING, acuteQWaveMyocardialInfarction.getId()));
	}

	@Test
	public void testMultiCohortStatisticalTesting() throws ServiceException {
		// Calculate the statistical chance of patients with hypertension encountering a heart attack

		// Within our data test how administering Paracetamol effects the statistical chance of patients with hypertension encountering a heart attack.
		// A. Count patients with hypertension and no Paracetamol
		// B. Count patients with hypertension and no Paracetamol and subsequent heart attack
		// Chance of heart attack without Paracetamol = B / A

		// C. Count patients with hypertension and Paracetamol
		// D. Count patients with hypertension and Paracetamol and subsequent heart attack
		//   Note: The Paracetamol prescription should have a date range but we haven't gone into that at this stage.
		// Chance of heart attack with Paracetamol = D / C

		CohortCriteria cohortCriteria = new CohortCriteria(new Criterion("<<" + hypertension.getId()));
		cohortCriteria.setTestVariable(new RelativeCriterion("<<" + paracetamol.getId().toString(), null, 5 * year));
		cohortCriteria.setTestOutcome(new RelativeCriterion("<<" + myocardialInfarction.getId().toString(), null, 5 * year));

		StatisticalTestResult result = queryService.fetchStatisticalTestResult(cohortCriteria);
		assertEquals(2, result.getHasTestVariableHasOutcomeCount());
		assertEquals(5, result.getHasTestVariableCount());
		assertEquals("0.4", "" + result.getHasTestVariableChanceOfOutcome());
		assertEquals(4, result.getHasNotTestVariableHasOutcomeCount());
		assertEquals(6, result.getHasNotTestVariableCount());
		assertEquals("0.67", "" + result.getHasNotTestVariableChanceOfOutcome());
	}

	private Patient createPatient(String roleId) {
		Patient patient = new Patient(roleId, "", TestUtils.getDob(35), Gender.FEMALE);
		healthDataStream.createPatient(patient);
		return patient;
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
