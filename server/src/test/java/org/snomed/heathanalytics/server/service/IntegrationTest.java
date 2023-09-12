package org.snomed.heathanalytics.server.service;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.metrics.ParsedScriptedMetric;
import org.ihtsdo.otf.snomedboot.factory.implementation.standard.ConceptImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.snomed.heathanalytics.model.ClinicalEncounter;
import org.snomed.heathanalytics.model.Gender;
import org.snomed.heathanalytics.model.Patient;
import org.snomed.heathanalytics.server.AbstractDataTest;
import org.snomed.heathanalytics.server.TestUtils;
import org.snomed.heathanalytics.server.ingestion.elasticsearch.ElasticOutputStream;
import org.snomed.heathanalytics.server.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.junit.Assert.*;

public class IntegrationTest extends AbstractDataTest {

	@Autowired
	private PatientQueryService patientQueryService;

	@Autowired
	private ReportService reportService;

	@MockBean
	private SnomedService snomedService;

	@Autowired
	private ElasticOutputStream healthDataStream;

	@Autowired
	private CPTService cptService;

	private final String hypertensionCode = "38341003";
	private ConceptImpl hypertension;
	private ConceptImpl myocardialInfarction;
	private ConceptImpl acuteQWaveMyocardialInfarction;
	private ConceptImpl breastScreening;
	private ConceptImpl breastMammography;
	private final String hypertensionDueToCongenitalAdrenalHyperplasiaExpression = hypertensionCode + ":{363713009=35105006,363714003=75367002},{42752001=237751000}";
	private final String breastScreeningId = "268547008";
	private final String breastMammographyId = "566571000119105";

	@BeforeEach
	public void setup() throws IOException, ParseException, ServiceException {
		// Set up ECL query service test data
		List<ConceptImpl> allConcepts = new ArrayList<>();

		// 38341003 |Hypertensive disorder, systemic arterial (disorder)|
		hypertension = newConcept(hypertensionCode, allConcepts);

		// 22298006 |Myocardial infarction (disorder)|
		myocardialInfarction = newConcept("22298006", allConcepts);

		// 304914007 |Acute Q wave myocardial infarction (disorder)|
		acuteQWaveMyocardialInfarction = newConcept("304914007", allConcepts);
		acuteQWaveMyocardialInfarction.addInferredParent(myocardialInfarction);

		// Breast cancer screening -
		// 268547008 | Screening for malignant neoplasm of breast (procedure) |
		breastScreening = newConcept(breastScreeningId, allConcepts);
		breastMammography = newConcept(breastMammographyId, allConcepts);

		Mockito.when(snomedService.getConceptIds(breastScreeningId)).thenReturn(List.of("268547008"));
		Mockito.when(snomedService.getConceptIds(breastMammographyId)).thenReturn(List.of("566571000119105"));
		Mockito.when(snomedService.getConceptIds("<<22298006")).thenReturn(List.of("22298006", "304914007"));
		Mockito.when(snomedService.getConceptIds("<<268547008")).thenReturn(List.of("268547008"));
		Mockito.when(snomedService.getConceptIds("<<" + hypertensionCode)).thenReturn(List.of(hypertensionCode, hypertensionDueToCongenitalAdrenalHyperplasiaExpression));

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
						.addEncounter(new ClinicalEncounter(TestUtils.date(2010, 5, 1), hypertensionDueToCongenitalAdrenalHyperplasiaExpression))
		);


		Long screeningId = breastScreening.getId();
		Long breastMammographyId = breastMammography.getId();
		// Ann - screening once a year, regular as clockwork
		healthDataStream.createPatient(
				new Patient("100", TestUtils.getDob(50), Gender.FEMALE)
						.addEncounter(new ClinicalEncounter(new GregorianCalendar(2017, Calendar.JUNE, 1), screeningId))
						.addEncounter(new ClinicalEncounter(new GregorianCalendar(2018, Calendar.JUNE, 1), screeningId))
						.addEncounter(new ClinicalEncounter(new GregorianCalendar(2019, Calendar.JUNE, 1), screeningId))
		);

		// Bella - screening once a year, varies by a month or so. Also one mammography.
		healthDataStream.createPatient(
				new Patient("101", TestUtils.getDob(52), Gender.FEMALE)
						.addEncounter(new ClinicalEncounter(new GregorianCalendar(2017, Calendar.MAY, 5), screeningId))
						.addEncounter(new ClinicalEncounter(new GregorianCalendar(2018, Calendar.APRIL, 10), screeningId))
						.addEncounter(new ClinicalEncounter(new GregorianCalendar(2019, Calendar.MAY, 20), screeningId))
						.addEncounter(new ClinicalEncounter(new GregorianCalendar(2019, Calendar.MAY, 20), breastMammographyId))
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

		Page<Patient> patients = patientQueryService.fetchCohort(cohortCriteria);

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
		Page<Patient> patients = patientQueryService.fetchCohort(patientCriteria);
		assertEquals("[1, 2]", toSortedPatientIdList(patients).toString());


		// Add inclusion criteria with Myocardial Infarction 5 days after primaryExposure
		EncounterCriterion inclusionCriteria = new EncounterCriterion("<<" + myocardialInfarction.getId().toString(), 5, null);
		patientCriteria.addEncounterCriterion(inclusionCriteria);

		// No matches because Bob had Myocardial Infarction 10 days after Hypertension is recorded
		patients = patientQueryService.fetchCohort(patientCriteria);
		assertEquals("[]", toSortedPatientIdList(patients).toString());

		// Extend search to 12 days after the primaryExposure
		inclusionCriteria.setWithinDaysAfterPreviouslyMatchedEncounter(12);

		// Now Bob is in the cohort
		patients = patientQueryService.fetchCohort(patientCriteria);
		assertEquals("[1]", toSortedPatientIdList(patients).toString());

		// Switch to exclude patients who had Myocardial Infarction
		inclusionCriteria.setHas(false);

		// Now only Dave is in
		patients = patientQueryService.fetchCohort(patientCriteria);
		assertEquals("[2]", toSortedPatientIdList(patients).toString());
	}

	@Test
	public void testGenderSelection() throws ServiceException {
		CohortCriteria cohortCriteria = new CohortCriteria(new EncounterCriterion("<<" + myocardialInfarction.getId().toString()));

		// No gender filter
		Page<Patient> patients = patientQueryService.fetchCohort(cohortCriteria);
		assertEquals(1, patients.getTotalElements());
		assertEquals(Gender.MALE, patients.iterator().next().getGender());

		// Females
		cohortCriteria.setGender(Gender.FEMALE);
		assertEquals(0, patientQueryService.fetchCohort(cohortCriteria).getTotalElements());

		// Males
		cohortCriteria.setGender(Gender.MALE);
		// TODO: Why doesn't this work?
//		assertEquals(1, queryService.fetchCohort(cohortCriteria).getTotalElements());
	}

	@Test
	public void testAgeSelection() throws ServiceException {
		CohortCriteria cohortCriteria = new CohortCriteria(new EncounterCriterion("<<" + myocardialInfarction.getId().toString()));

		// No age filter
		assertEquals(1, patientQueryService.fetchCohort(cohortCriteria).getTotalElements());

		cohortCriteria.setMinAgeNow(36);
		assertEquals(0, patientQueryService.fetchCohort(cohortCriteria).getTotalElements());

		cohortCriteria.setMinAgeNow(35);
		assertEquals(1, patientQueryService.fetchCohort(cohortCriteria).getTotalElements());

		cohortCriteria.setMinAgeNow(30);
		cohortCriteria.setMaxAgeNow(31);
		assertEquals(0, patientQueryService.fetchCohort(cohortCriteria).getTotalElements());

		cohortCriteria.setMinAgeNow(30);
		cohortCriteria.setMaxAgeNow(38);
		assertEquals(1, patientQueryService.fetchCohort(cohortCriteria).getTotalElements());
	}

	@Test
	public void testTreatmentDateRange() throws ServiceException {
		EncounterCriterion encounterCriterion = new EncounterCriterion(breastScreeningId);
		assertEquals(4, patientQueryService.fetchCohortCount(new CohortCriteria(encounterCriterion)));

		encounterCriterion.setMinDate(new GregorianCalendar(2019, Calendar.JANUARY, 1).getTime());
		assertTrue(encounterCriterion.hasTimeConstraint());
		assertEquals(3, patientQueryService.fetchCohortCount(new CohortCriteria(encounterCriterion)));

		encounterCriterion.setMaxDate(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
		assertTrue(encounterCriterion.hasTimeConstraint());
		assertEquals(3, patientQueryService.fetchCohortCount(new CohortCriteria(encounterCriterion)));

		encounterCriterion.setMaxDate(new GregorianCalendar(2019, Calendar.JANUARY, 1).getTime());
		assertTrue(encounterCriterion.hasTimeConstraint());
		assertEquals(0, patientQueryService.fetchCohortCount(new CohortCriteria(encounterCriterion)));
	}

	@Test
	public void testTreatmentFrequencySelection() throws ServiceException {
		assertEquals(6, patientQueryService.fetchCohortCount(new CohortCriteria()));
		assertEquals(4, patientQueryService.fetchCohortCount(new CohortCriteria().setGender(Gender.FEMALE)));
		assertEquals(4, patientQueryService.fetchCohortCount(new CohortCriteria(new EncounterCriterion(breastScreeningId))));

		// At least 2 screens 10-14 months apart
		assertEquals(2, patientQueryService.fetchCohortCount(new CohortCriteria()
				.addEncounterCriterion(new EncounterCriterion(breastScreeningId).setFrequency(new Frequency(2, 10, 14, TimeUnit.MONTH)))
		));

		// 2 screens and at least one mammography
		assertEquals(1, patientQueryService.fetchCohortCount(new CohortCriteria()
				.addEncounterCriterion(new EncounterCriterion(breastScreeningId).setFrequency(new Frequency(2, 10, 14, TimeUnit.MONTH)))
				.addEncounterCriterion(new EncounterCriterion(breastMammographyId))
		));

		// At least 3 screens 10-14 months apart
		assertEquals(2, patientQueryService.fetchCohortCount(new CohortCriteria(new EncounterCriterion(breastScreeningId)
				.setFrequency(new Frequency(3, 10, 14, TimeUnit.MONTH)))));

		// At least 2 screens 22-26 months apart
		assertEquals(1, patientQueryService.fetchCohortCount(new CohortCriteria(new EncounterCriterion(breastScreeningId)
				.setFrequency(new Frequency(2, 22, 26, TimeUnit.MONTH)))));
	}

	@Test
	public void testCohortExclusion() throws ServiceException {
		// criteria for breast screening
		CohortCriteria cohortCriteria = new CohortCriteria(new EncounterCriterion("<<" + breastScreening.getId().toString()));

		// No filter
		assertEquals(4, patientQueryService.fetchCohort(cohortCriteria).getTotalElements());

		cohortCriteria.setMinAgeNow(40);
		cohortCriteria.setMaxAgeNow(60);
		assertEquals(4, patientQueryService.fetchCohort(cohortCriteria).getTotalElements());

		// Use ExclusionCriteria to exclude patients between 50 and 51 years old
		cohortCriteria.getExclusionCriteria().add(new CohortCriteria(null, 50, 51));
		assertEquals(3, patientQueryService.fetchCohort(cohortCriteria).getTotalElements());

		// Use ExclusionCriteria to exclude patients with at least 2 screens 22-26 months apart
		cohortCriteria.getExclusionCriteria().add(new CohortCriteria(new EncounterCriterion(breastScreeningId)
				.setFrequency(new Frequency(2, 22, 26, TimeUnit.MONTH))));

		assertEquals(2, patientQueryService.fetchCohort(cohortCriteria).getTotalElements());
	}

	@Test
	public void testReportWithCPTAnalysis() throws ServiceException {
		Map<String, CPTCode> snomedToCptMap = cptService.getSnomedToCptMap();
		CPTCode dummyCpt = snomedToCptMap.get(breastScreeningId);
		assertNotNull(dummyCpt);
		String screeningDummyCPT = "12345";
		assertEquals(screeningDummyCPT, dummyCpt.getCptCode());

		ReportDefinition reportDefinition = new ReportDefinition()
				.addReportToFirstListOfGroups(new SubReportDefinition("Screens", new CohortCriteria(new EncounterCriterion(breastScreeningId)
						.includeCPTAnalysis())));

		Report report = reportService.runReport(reportDefinition);
		List<Report> groups = report.getGroups();
		assertEquals(1, groups.size());
		Report screensReport = groups.get(0);
		assertEquals("Screens", screensReport.getName());
		assertEquals(4, screensReport.getPatientCount());
		Map<String, CPTTotals> cptTotals = screensReport.getCptTotals();
		assertNotNull(cptTotals);
		assertEquals(1, cptTotals.size());
		CPTTotals actual = cptTotals.get(screeningDummyCPT);
		assertNotNull(actual);
		assertEquals(4, actual.getCount());
		assertEquals(Float.valueOf(12.6F), actual.getWorkRVU());

		// Frequency filter must reduce CPT aggregation counts
		reportDefinition = new ReportDefinition()
				.addReportToFirstListOfGroups(new SubReportDefinition("Screens", new CohortCriteria(new EncounterCriterion(breastScreeningId)
						.setFrequency(new Frequency(2, 10, 14, TimeUnit.MONTH))
						.includeCPTAnalysis())));
		report = reportService.runReport(reportDefinition);
		groups = report.getGroups();
		assertEquals(1, groups.size());
		screensReport = groups.get(0);
		assertEquals("Screens", screensReport.getName());
		cptTotals = screensReport.getCptTotals();
		assertNotNull(cptTotals);
		assertEquals(1, cptTotals.size());
		actual = cptTotals.get(screeningDummyCPT);
		assertNotNull(actual);
		assertEquals(2, actual.getCount());
		// TODO: fix rounding
		assertEquals(Float.valueOf(6.3F), actual.getWorkRVU());
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

	// Method for manual hacking/testing against a local instance
	public static void main(String[] args) {
		ElasticsearchRestTemplate restTemplate = new ElasticsearchRestTemplate(new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200))));
		Set<String> includeConcepts = new HashSet<>();
		String code = "304914007";
		includeConcepts.add(code);
		Map<String, Object> params = new HashMap<>();
		params.put("includeConcepts", includeConcepts);
		NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder()
				.addAggregation(
					AggregationBuilders.scriptedMetric("encounterConceptCounts")
							.initScript(new Script("state.concepts = new HashMap()"))
							.mapScript(new Script(ScriptType.INLINE, "painless",
									"for (int i = 0; i < doc['encounters.conceptId.keyword'].length; i++) {" +
									"	Map concepts = state.concepts;" +
									"	String conceptId = doc['encounters.conceptId.keyword'][i];" +
									"	if (params.includeConcepts.contains(conceptId)) {" +
									"		if (concepts.containsKey(conceptId)) {" +
									"			long count = concepts.get(conceptId).longValue() + 1L;" +
									"			concepts.put(conceptId, count);" +
									"		} else {" +
									"			concepts.put(conceptId, 1L);" +
									"		}" +
									"	}" +
									"}", params))
							.combineScript(new Script("return state;"))
							.reduceScript(new Script("Map allConcepts = new HashMap();" +
									"for (state in states) {" +
									"	if (state.concepts != null) {" +
									"		for (conceptId in state.concepts.keySet()) {" +
									"			if (allConcepts.containsKey(conceptId)) {" +
									"				long count = allConcepts.get(conceptId) + state.concepts.get(conceptId);" +
									"				allConcepts.put(conceptId, count);" +
									"			} else {" +
									"				allConcepts.put(conceptId, state.concepts.get(conceptId));" +
									"			}" +
									"		}" +
									"	}" +
									"}" +
									"return allConcepts;")));
		SearchHits<Patient> hits = restTemplate.search(nativeSearchQueryBuilder.build(), Patient.class);
		System.out.println(hits.getTotalHits() + " hits");
		Aggregations aggregations = hits.getAggregations();
		ParsedScriptedMetric encounterConceptCounts = aggregations.get("encounterConceptCounts");
		System.out.println(encounterConceptCounts);
		@SuppressWarnings("unchecked")
		Map<String, Long> conceptCounts = (Map<String, Long>) encounterConceptCounts.aggregation();
		System.out.println(conceptCounts);
		System.out.println("Done");
		System.exit(0);
	}

}
