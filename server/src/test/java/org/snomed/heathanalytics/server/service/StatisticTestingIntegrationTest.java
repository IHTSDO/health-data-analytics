package org.snomed.heathanalytics.server.service;

import org.ihtsdo.otf.snomedboot.factory.implementation.standard.ConceptImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.snomed.heathanalytics.model.ClinicalEvent;
import org.snomed.heathanalytics.model.Gender;
import org.snomed.heathanalytics.model.Patient;
import org.snomed.heathanalytics.server.AbstractDataTest;
import org.snomed.heathanalytics.server.TestUtils;
import org.snomed.heathanalytics.server.ingestion.elasticsearch.ElasticOutputStream;
import org.snomed.heathanalytics.server.model.CohortCriteria;
import org.snomed.heathanalytics.server.model.EventCriterion;
import org.snomed.heathanalytics.server.model.StatisticalCorrelationReport;
import org.snomed.heathanalytics.server.model.StatisticalCorrelationReportDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.snomed.heathanalytics.server.TestUtils.date;

public class StatisticTestingIntegrationTest extends AbstractDataTest {

	@MockBean
	private SnomedService snomedService;

	@Autowired
	private ReportService reportService;

	@Autowired
	private ElasticOutputStream healthDataStream;

	private ConceptImpl hypertension;
	private ConceptImpl diabetes;
	private ConceptImpl myocardialInfarction;
	private ConceptImpl acuteQWaveMyocardialInfarction;
	private ConceptImpl paracetamol;

	private static final int YEAR_IN_DAYS = 365;

	@BeforeEach
	public void setup() throws ServiceException {
		// Set up ECL query service test data
		List<ConceptImpl> allConcepts = new ArrayList<>();

		// 38341003 |Hypertensive disorder, systemic arterial (disorder)|
		hypertension = createConcept("38341003", allConcepts);

		// 73211009 |Diabetes mellitus (disorder)|
		diabetes = createConcept("73211009", allConcepts);

		// 22298006 |Myocardial infarction (disorder)|
		// Also known as "heart attack"
		myocardialInfarction = createConcept("22298006", allConcepts);

		// 304914007 |Acute Q wave myocardial infarction (disorder)|
		acuteQWaveMyocardialInfarction = createConcept("304914007", allConcepts);
		acuteQWaveMyocardialInfarction.addInferredParent(myocardialInfarction);

		// 51234001 |Paracetamol|
		paracetamol = createConcept("51234001", allConcepts);

		Mockito.when(snomedService.getConceptIds("<<38341003")).thenReturn(Collections.singletonList(38341003L));
		Mockito.when(snomedService.getConceptIds("<<51234001")).thenReturn(Collections.singletonList(51234001L));
		Mockito.when(snomedService.getConceptIds("<<22298006")).thenReturn(List.of(22298006L, 304914007L));

		//
		// Set up some data for this test
		//

		// 1 patient without hypertension and with heart attack
		Patient patient = createPatient("1");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2015, 2, 1), acuteQWaveMyocardialInfarction.getId()), "A");

		// 2 patients with hypertension and no Paracetamol and no heart attack
		patient = createPatient("2");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2017, 0, 10), hypertension.getId()), "A");
		patient = createPatient("3");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2017, 0, 15), hypertension.getId()), "A");

		// 4 patients with hypertension and no Paracetamol and subsequent heart attack
		patient = createPatient("4");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2015, 0, 1), hypertension.getId()), "A");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2017, 0, 1), acuteQWaveMyocardialInfarction.getId()), "A");
		patient = createPatient("5");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2016, 0, 1), hypertension.getId()), "A");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2017, 0, 1), acuteQWaveMyocardialInfarction.getId()), "A");
		patient = createPatient("6");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2015, 5, 1), hypertension.getId()), "A");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2017, 2, 1), acuteQWaveMyocardialInfarction.getId()), "A");
		patient = createPatient("7");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2015, 1, 1), hypertension.getId()), "A");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2017, 1, 1), acuteQWaveMyocardialInfarction.getId()), "A");

		// 3 patients with hypertension and Paracetamol and no heart attack
		patient = createPatient("8");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2017, 0, 10), hypertension.getId()), "A");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2017, 3, 10), paracetamol.getId()), "A");
		patient = createPatient("9");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2017, 0, 15), hypertension.getId()), "A");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2017, 3, 10), paracetamol.getId()), "A");
		patient = createPatient("10");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2015, 0, 1), hypertension.getId()), "A");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2017, 3, 10), paracetamol.getId()), "A");

		// 2 patients with hypertension, diabetes and Paracetamol and subsequent heart attack
		patient = createPatient("11");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2016, 0, 1), hypertension.getId()), "A");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2016, 0, 1), diabetes.getId()), "A");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2016, 3, 10), paracetamol.getId()), "A");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2017, 0, 1), acuteQWaveMyocardialInfarction.getId()), "A");
		patient = createPatient("12");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2016, 0, 1), hypertension.getId()), "A");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2016, 0, 1), diabetes.getId()), "A");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2016, 3, 10), paracetamol.getId()), "A");
		healthDataStream.addClinicalEvent(patient.getRoleId(), new ClinicalEvent(date(2017, 2, 1), acuteQWaveMyocardialInfarction.getId()), "A");
		healthDataStream.flush();
	}

	@Test
	public void testMultiCohortStatisticalTest() throws ServiceException {
		// Calculate the statistical chance of patients with hypertension eventing a heart attack

		// Within our data test how administering Paracetamol effects the statistical chance of patients with hypertension eventing a heart attack.
		// A. Count patients with hypertension and no Paracetamol
		// B. Count patients with hypertension and no Paracetamol and subsequent heart attack
		// Chance of heart attack without Paracetamol = B / A

		// C. Count patients with hypertension and Paracetamol
		// D. Count patients with hypertension and Paracetamol and subsequent heart attack
		//   Note: The Paracetamol prescription should have a date range but we haven't gone into that at this stage.
		// Chance of heart attack with Paracetamol = D / C

		StatisticalCorrelationReportDefinition statisticalCorrelationReportDefinition = new StatisticalCorrelationReportDefinition(new CohortCriteria("A", new EventCriterion("<<" + hypertension.getId())),
				new EventCriterion("<<" + paracetamol.getId().toString(), 5 * YEAR_IN_DAYS, null),
				new EventCriterion("<<" + myocardialInfarction.getId().toString(), 5 * YEAR_IN_DAYS, null));

		StatisticalCorrelationReport result = reportService.runStatisticalReport(statisticalCorrelationReportDefinition);
		assertEquals(5, result.getWithTreatmentCount());
		assertEquals(2, result.getWithTreatmentWithNegativeOutcomeCount());
		assertEquals("40.0", result.getWithTreatmentChanceOfNegativeOutcome());
		assertEquals(6, result.getWithoutTreatmentCount());
		assertEquals(4, result.getWithoutTreatmentWithNegativeOutcomeCount());
		assertEquals("66.7", result.getWithoutTreatmentChanceOfNegativeOutcome());
	}

	@Test
	public void testMultiCohortStatisticalTestWithRefinement() throws ServiceException {
		CohortCriteria patientCriteria = new CohortCriteria("A", new EventCriterion("<<" + hypertension.getId()));

		// Here is the additional criterion
		patientCriteria.addEventCriterion(new EventCriterion("<<" + myocardialInfarction.getId().toString(), -1, null));

		// The additional criterion means that our cohort is reduced to 2 patients, both the the same outcome.
		EventCriterion treatmentCriterion = new EventCriterion("<<" + paracetamol.getId().toString(), 5 * YEAR_IN_DAYS, null);
		EventCriterion negativeOutcomeCriterion = new EventCriterion("<<" + myocardialInfarction.getId().toString(), 5 * YEAR_IN_DAYS, null);

		StatisticalCorrelationReport result = reportService.runStatisticalReport(new StatisticalCorrelationReportDefinition(patientCriteria, treatmentCriterion, negativeOutcomeCriterion));

		assertEquals(2, result.getWithTreatmentWithNegativeOutcomeCount());
		assertEquals(2, result.getWithTreatmentCount());
		assertEquals("100.0", result.getWithTreatmentChanceOfNegativeOutcome());
		assertEquals(4, result.getWithoutTreatmentWithNegativeOutcomeCount());
		assertEquals(4, result.getWithoutTreatmentCount());
		assertEquals("100.0", result.getWithoutTreatmentChanceOfNegativeOutcome());
	}

	private Patient createPatient(String roleId) {
		Patient patient = new Patient(roleId, TestUtils.getDob(35), Gender.FEMALE);
		healthDataStream.createPatient(patient, "A");
		return patient;
	}

	private ConceptImpl createConcept(String id, List<ConceptImpl> allConcepts) {
		ConceptImpl concept = new ConceptImpl(id, "20170731", true, "", "");
		concept.setFsn("");
		allConcepts.add(concept);
		return concept;
	}

}
