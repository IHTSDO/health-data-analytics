package org.snomed.heathanalytics.server.service;

import org.ihtsdo.otf.snomedboot.factory.implementation.standard.ConceptImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.snomed.heathanalytics.model.ClinicalEncounter;
import org.snomed.heathanalytics.model.Gender;
import org.snomed.heathanalytics.model.Patient;
import org.snomed.heathanalytics.server.TestConfig;
import org.snomed.heathanalytics.server.TestUtils;
import org.snomed.heathanalytics.server.ingestion.elasticsearch.ElasticOutputStream;
import org.snomed.heathanalytics.server.model.*;
import org.snomed.heathanalytics.server.store.PatientRepository;
import org.snomed.heathanalytics.server.testutil.TestSnomedQueryServiceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class IntegrationTest {

	@Autowired
	private QueryService queryService;

	@Autowired
	private ReportService reportService;

	@Autowired
	private SnomedService snomedService;

	@Autowired
	private ElasticOutputStream healthDataStream;

	@Autowired
	private PatientRepository patientRepository;

	@Autowired
	private CPTService cptService;

	private ConceptImpl hypertension;
	private ConceptImpl myocardialInfarction;
	private ConceptImpl acuteQWaveMyocardialInfarction;
	private ConceptImpl breastCancerScreening;
	private final String breastCancerScreeningId = "268547008";

	@Before
	public void setup() throws IOException, ParseException {
		// Set up ECL query service test data
		List<ConceptImpl> allConcepts = new ArrayList<>();

		// 38341003 |Hypertensive disorder, systemic arterial (disorder)|
		hypertension = newConcept("38341003", allConcepts);

		// 22298006 |Myocardial infarction (disorder)|
		myocardialInfarction = newConcept("22298006", allConcepts);

		// 304914007 |Acute Q wave myocardial infarction (disorder)|
		acuteQWaveMyocardialInfarction = newConcept("304914007", allConcepts);
		acuteQWaveMyocardialInfarction.addInferredParent(myocardialInfarction);

		// Breast cancer screening -
		// 268547008 | Screening for malignant neoplasm of breast (procedure) |
		breastCancerScreening = newConcept(breastCancerScreeningId, allConcepts);

		snomedService.setSnomedQueryService(TestSnomedQueryServiceBuilder.createWithConcepts(allConcepts.toArray(new ConceptImpl[]{})));


		// Set up tiny set of integration test data.
		// There is no attempt to make this realistic, we are just testing the logic.

		// Bob
		// has hypertension.
		// has a type of myocardial infarction 10 days after the hypertension was recorded.
		healthDataStream.createPatient(
				new Patient("1", TestUtils.getDob(35), Gender.MALE)
						.addEncounter(new ClinicalEncounter(TestUtils.date(2017, 0, 10), hypertension.getId()))
						.addEncounter(new ClinicalEncounter(TestUtils.date(2017, 0, 20), acuteQWaveMyocardialInfarction.getId()))
		);

		// Dave
		// has hypertension. No other recorded disorders.
		healthDataStream.createPatient(
				new Patient("2", TestUtils.getDob(40), Gender.MALE)
						.addEncounter(new ClinicalEncounter(TestUtils.date(2010, 5, 1), hypertension.getId()))
		);


		Long screeningId = breastCancerScreening.getId();
		// Ann - screening once a year, regular as clockwork
		healthDataStream.createPatient(
				new Patient("100", TestUtils.getDob(50), Gender.FEMALE)
						.addEncounter(new ClinicalEncounter(new GregorianCalendar(2017, Calendar.JUNE, 1), screeningId))
						.addEncounter(new ClinicalEncounter(new GregorianCalendar(2018, Calendar.JUNE, 1), screeningId))
						.addEncounter(new ClinicalEncounter(new GregorianCalendar(2019, Calendar.JUNE, 1), screeningId))
		);

		// Bella - screening once a year, varies by a month or so.
		healthDataStream.createPatient(
				new Patient("101", TestUtils.getDob(52), Gender.FEMALE)
						.addEncounter(new ClinicalEncounter(new GregorianCalendar(2017, Calendar.MAY, 5), screeningId))
						.addEncounter(new ClinicalEncounter(new GregorianCalendar(2018, Calendar.APRIL, 10), screeningId))
						.addEncounter(new ClinicalEncounter(new GregorianCalendar(2019, Calendar.MAY, 20), screeningId))
		);

		// Claudia - screening once every two years, varies by a couple of weeks.
		healthDataStream.createPatient(
				new Patient("102", TestUtils.getDob(52), Gender.FEMALE)
						.addEncounter(new ClinicalEncounter(new GregorianCalendar(2015, Calendar.JANUARY, 10), screeningId))
						.addEncounter(new ClinicalEncounter(new GregorianCalendar(2017, Calendar.JANUARY, 5), screeningId))
						.addEncounter(new ClinicalEncounter(new GregorianCalendar(2019, Calendar.JANUARY, 20), screeningId))
		);

		// Diane - screens three years apart
		healthDataStream.createPatient(
				new Patient("103", TestUtils.getDob(52), Gender.FEMALE)
						.addEncounter(new ClinicalEncounter(new GregorianCalendar(2015, Calendar.JANUARY, 10), screeningId))
						.addEncounter(new ClinicalEncounter(new GregorianCalendar(2018, Calendar.JANUARY, 5), screeningId))
		);

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

	@Test
	public void testTreatmentFrequencySelection() throws ServiceException {
		assertEquals(6, queryService.fetchCohortCount(new CohortCriteria()));
		assertEquals(4, queryService.fetchCohortCount(new CohortCriteria().setGender(Gender.FEMALE)));
		assertEquals(4, queryService.fetchCohortCount(new CohortCriteria(new EncounterCriterion(breastCancerScreeningId))));

		// At least 2 screens 10-14 months apart
		assertEquals(2, queryService.fetchCohortCount(new CohortCriteria(new EncounterCriterion(breastCancerScreeningId)
				.setFrequency(new Frequency(2, 10, 14, TimeUnit.MONTH)))));

		// At least 3 screens 10-14 months apart
		assertEquals(2, queryService.fetchCohortCount(new CohortCriteria(new EncounterCriterion(breastCancerScreeningId)
				.setFrequency(new Frequency(3, 10, 14, TimeUnit.MONTH)))));

		// At least 2 screens 22-26 months apart
		assertEquals(1, queryService.fetchCohortCount(new CohortCriteria(new EncounterCriterion(breastCancerScreeningId)
				.setFrequency(new Frequency(2, 22, 26, TimeUnit.MONTH)))));
	}

	@Test
	public void testReportWithCPTAnalysis() throws ServiceException {
		Map<String, CPTCode> snomedToCptMap = cptService.getSnomedToCptMap();
		CPTCode dummyCpt = snomedToCptMap.get(breastCancerScreeningId);
		assertNotNull(dummyCpt);
		String screeningDummyCPT = "12345";
		assertEquals(screeningDummyCPT, dummyCpt.getCptCode());

		ReportDefinition reportDefinition = new ReportDefinition()
				.addReportToFirstListOfGroups(new SubReportDefinition("Screens", new CohortCriteria(new EncounterCriterion(breastCancerScreeningId)
						.includeCPTAnalysis())));

		Report report = reportService.runReport(reportDefinition);
		List<Report> groups = report.getGroups();
		System.out.println(groups);
		assertEquals(1, groups.size());
		Report screensReport = groups.get(0);
		assertEquals("Screens", screensReport.getName());
		Map<String, CPTTotals> cptTotals = screensReport.getCptTotals();
		System.out.println(cptTotals.size());
		System.out.println(cptTotals);
		assertEquals(1, cptTotals.size());
		CPTTotals actual = cptTotals.get(screeningDummyCPT);
		assertNotNull(actual);
		assertEquals(4, actual.getCount());
		assertEquals(new Float(12.6), actual.getWorkRVU());
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
		patientRepository.deleteAll();
	}

}
