package org.snomed.heathanalytics.server.service;

import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.metrics.ParsedScriptedMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.model.ClinicalEncounter;
import org.snomed.heathanalytics.model.Gender;
import org.snomed.heathanalytics.model.Patient;
import org.snomed.heathanalytics.model.pojo.TermHolder;
import org.snomed.heathanalytics.server.model.*;
import org.snomed.heathanalytics.server.pojo.Stats;
import org.snomed.heathanalytics.server.store.SubsetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.elasticsearch.index.query.QueryBuilders.*;

@Service
public class PatientQueryService {

	@Autowired
	private SnomedService snomedService;

	@Autowired
	private ElasticsearchOperations elasticsearchTemplate;

	@Autowired
	private SubsetRepository subsetRepository;

	@Autowired
	private CPTService cptService;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public Stats getStats() {
		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().build();
		long patientCount = elasticsearchTemplate.count(searchQuery, Patient.class);
		return new Stats(new Date(), patientCount);
	}

	public int fetchCohortCount(CohortCriteria patientCriteria) throws ServiceException {
		return (int) fetchCohort(patientCriteria, 0, 1).getTotalElements();
	}

	public Page<Patient> fetchCohort(CohortCriteria cohortCriteria) throws ServiceException {
		return fetchCohort(cohortCriteria, 0, 100);
	}

	public Page<Patient> fetchCohort(CohortCriteria cohortCriteria, int page, int size) throws ServiceException {
		return doFetchCohort(cohortCriteria, page, size, new GregorianCalendar(), new Timer());
	}

	private Page<Patient> doFetchCohort(CohortCriteria patientCriteria, int page, int size, GregorianCalendar now, Timer timer) throws ServiceException {

		validateCriteria(patientCriteria);

		BoolQueryBuilder patientQuery = getPatientClauses(patientCriteria.getGender(), patientCriteria.getMinAgeNow(), patientCriteria.getMaxAgeNow(), now);
		List<EncounterCriterion> encounterCriteria = patientCriteria.getEncounterCriteria();

		// Fetch conceptIds of each criterion
		Map<String, List<String>> eclToConceptsMap = new HashMap<>();
		List<EncounterCriterion> allEncounterCriteria = new ArrayList<>(encounterCriteria);
		List<CohortCriteria> exclusionCriteria = patientCriteria.getExclusionCriteria();
		exclusionCriteria.forEach(excludeCohort -> allEncounterCriteria.addAll(excludeCohort.getEncounterCriteria()));
		for (EncounterCriterion criterion : allEncounterCriteria) {
			String criterionEcl = getGivenOrSubsetEcl(criterion);
			if (criterionEcl != null) {
				if (!eclToConceptsMap.containsKey(criterionEcl)) {
					timer.split("Fetching concepts for ECL " + criterionEcl);
					eclToConceptsMap.put(criterionEcl, snomedService.getConceptIds(criterionEcl));
				}
			}
		}

		BoolQueryBuilder filterBoolBuilder = boolQuery();
		filterBoolBuilder.must(getPatientEncounterFilter(encounterCriteria, eclToConceptsMap));
		for (CohortCriteria exclusionCriterion : exclusionCriteria) {
			BoolQueryBuilder exclusionBool = boolQuery();
			exclusionBool.must(getPatientClauses(exclusionCriterion.getGender(), exclusionCriterion.getMinAgeNow(), exclusionCriterion.getMaxAgeNow(), now));
			if (!exclusionCriterion.getEncounterCriteria().isEmpty()) {
				exclusionBool.must(getPatientEncounterFilter(exclusionCriterion.getEncounterCriteria(), eclToConceptsMap));
			}
			filterBoolBuilder.mustNot(exclusionBool);
		}
		patientQuery.filter(filterBoolBuilder);

		Map<String, TermHolder> conceptTerms = new HashMap<>();
		PageRequest pageable = PageRequest.of(page, size);
		NativeSearchQueryBuilder patientElasticQuery = new NativeSearchQueryBuilder()
				.withQuery(patientQuery)
				.withPageable(pageable);

		List<EncounterCriterion> encounterCriteriaWithCPTAnalysis = encounterCriteria.stream().filter(EncounterCriterion::isIncludeCPTAnalysis).collect(Collectors.toList());
		if (!encounterCriteriaWithCPTAnalysis.isEmpty()) {
			Set<String> includeConcepts = new HashSet<>();
			for (EncounterCriterion criterion : encounterCriteriaWithCPTAnalysis) {
				List<String> concepts = eclToConceptsMap.get(criterion.getConceptECL());
				includeConcepts.addAll(concepts);
			}
			Map<String, Object> params = new HashMap<>();
			params.put("includeConcepts", includeConcepts);
			patientElasticQuery.addAggregation(
					AggregationBuilders.scriptedMetric("encounterConceptCounts")
							.initScript(new Script(ScriptType.INLINE, "painless",
									"state.concepts = new HashMap();" +
									"state.includeConcepts = params.includeConcepts;", params))
							.mapScript(new Script(
									"Map concepts = state.concepts;" +
									"for (String conceptId : doc['encounters.conceptId.keyword']) {" +
									"	if (state.includeConcepts.contains(conceptId)) {" +
									"		if (concepts.containsKey(conceptId)) {" +
									"			long count = concepts.get(conceptId).longValue() + 1L;" +
									"			concepts.put(conceptId, count);" +
									"		} else {" +
									"			concepts.put(conceptId, 1L);" +
									"		}" +
									"	}" +
									"}"))
							.combineScript(new Script("return state;"))
							.reduceScript(new Script(
									"Map allConcepts = new HashMap();" +
									"for (state in states) {" +
									"	if (state != null && state.concepts != null) {" +
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
		}

		// Grab page of Patients from Elasticsearch.
		Query query = patientElasticQuery.build();
		query.setTrackTotalHits(true);
		SearchHits<Patient> searchHits = elasticsearchTemplate.search(query, Patient.class);
		List<Patient> content = searchHits.stream().map(SearchHit::getContent).collect(Collectors.toList());
		Page<Patient> patients = new PageImpl<>(content, query.getPageable(), searchHits.getTotalHits());

		timer.split("Fetching patients");
		if (!encounterCriteriaWithCPTAnalysis.isEmpty()) {

			Aggregations aggregations = searchHits.getAggregations();
			Map<String, Aggregation> stringAggregationMap = aggregations.asMap();
			ParsedScriptedMetric encounterConceptCounts = (ParsedScriptedMetric) stringAggregationMap.get("encounterConceptCounts");
			Object aggregation = encounterConceptCounts.aggregation();
			@SuppressWarnings("unchecked")
			Map<String, Integer> conceptCounts = (Map<String, Integer>) aggregation;
			Map<String, CPTCode> snomedToCptMap = cptService.getSnomedToCptMap();
			Map<String, CPTTotals> cptTotalsMap = new HashMap<>();
			for (String conceptId : conceptCounts.keySet()) {
				CPTCode cptCode = snomedToCptMap.get(conceptId);
				if (cptCode != null) {
					cptTotalsMap.computeIfAbsent(cptCode.getCptCode(), (c) -> new CPTTotals(cptCode)).addCount(conceptCounts.get(conceptId));
				}
			}
			patients = new PatientPageWithCPTTotals(patients.getContent(), pageable, patients.getTotalElements(), cptTotalsMap);
		}

		// Process matching patients for display
		patients.getContent().forEach(patient -> {
			if (patient.getEncounters() == null) {
				patient.setEncounters(Collections.emptySet());
			}
			patient.getEncounters().forEach(encounter ->
					encounter.setConceptTerm(conceptTerms.computeIfAbsent(encounter.getConceptId(), conceptId -> new TermHolder())));
		});
		logger.info("Times: {}", timer.getTimes());
		return patients;
	}

	private void validateCriteria(CohortCriteria patientCriteria) {
		doValidateCriteria("", patientCriteria);
		for (int i = 0; i < patientCriteria.getExclusionCriteria().size(); i++) {
			CohortCriteria excludeCohort = patientCriteria.getExclusionCriteria().get(i);
			doValidateCriteria(format("ExcludeCohort[%s].", i), excludeCohort);
			if (!excludeCohort.getExclusionCriteria().isEmpty()) {
				throw new IllegalArgumentException("An ExcludeCohort may not have a further ExcludeCohort. Nested ExcludeCohorts are not supported.");
			}
		}
	}

	private void doValidateCriteria(String prefix, CohortCriteria patientCriteria) {
		List<EncounterCriterion> encounterCriteria = patientCriteria.getEncounterCriteria();
		for (int i = 0; i < encounterCriteria.size(); i++) {
			EncounterCriterion encounterCriterion = encounterCriteria.get(i);
			if ((Strings.isNullOrEmpty(encounterCriterion.getConceptECL()) && Strings.isNullOrEmpty(encounterCriterion.getConceptSubsetId()))
					|| (!Strings.isNullOrEmpty(encounterCriterion.getConceptECL()) && !Strings.isNullOrEmpty(encounterCriterion.getConceptSubsetId()))) {
				throw new IllegalArgumentException(format("%sEncounterCriterion[%s] must have either conceptECL or conceptSubsetId.", prefix, i));
			}
			Frequency frequency = encounterCriterion.getFrequency();
			if (frequency != null) {
				if (!encounterCriterion.isHas()) {
					throw new IllegalArgumentException(format("%sEncounterCriterion[%s].frequency can only be used when has=true.", prefix, i));
				}
				if (frequency.getMinRepetitions() == null || frequency.getMinRepetitions() < 1) {
					throw new IllegalArgumentException(format("%sEncounterCriterion[%s].frequency.minRepetitions must be a positive integer greater than 1.", prefix, i));
				}
				if (frequency.getMinTimeBetween() != null && frequency.getMinTimeBetween() < 0) {
					throw new IllegalArgumentException(format("%sEncounterCriterion[%s].frequency.minTimeBetween must be a positive integer.", prefix, i));
				}
				if (frequency.getMaxTimeBetween() != null && frequency.getMaxTimeBetween() < 0) {
					throw new IllegalArgumentException(format("%sEncounterCriterion[%s].frequency.maxTimeBetween must be a positive integer.", prefix, i));
				}
				if (frequency.getMinTimeBetween() != null && frequency.getMaxTimeBetween() != null
						&& frequency.getMinTimeBetween() > frequency.getMaxTimeBetween()) {
					throw new IllegalArgumentException(format("%sEncounterCriterion[%s].frequency.minTimeBetween must be less than maxTimeBetween.", prefix, i));
				}
				if (frequency.getTimeUnit() == null && (frequency.getMinTimeBetween() != null || frequency.getMaxTimeBetween() != null)) {
					throw new IllegalArgumentException(format("%sEncounterCriterion[%s].frequency.timeUnit is required when minTimeBetween or maxTimeBetween is set.", prefix, i));
				}
			}
		}
	}

	private BoolQueryBuilder getPatientClauses(Gender gender, Integer minAgeNow, Integer maxAgeNow, GregorianCalendar now) {
		BoolQueryBuilder patientQuery = boolQuery();
		if (gender != null) {
			patientQuery.must(termQuery(Patient.Fields.GENDER, gender));
		}
		if (minAgeNow != null || maxAgeNow != null) {
			// Crude match using birth year
			RangeQueryBuilder rangeQueryBuilder = rangeQuery(Patient.Fields.DOB_YEAR);
			int thisYear = now.get(Calendar.YEAR);
			if (minAgeNow != null) {
				rangeQueryBuilder.lte(thisYear - minAgeNow);
			}
			if (maxAgeNow != null) {
				rangeQueryBuilder.gte(thisYear - maxAgeNow);
			}
			patientQuery.must(rangeQueryBuilder);
		}
		return patientQuery;
	}

	private BoolQueryBuilder getPatientEncounterFilter(List<EncounterCriterion> encounterCriteria, Map<String, List<String>> eclToConceptsMap) throws ServiceException {
		BoolQueryBuilder patientFilter = boolQuery();

		// Fetch conceptIds of each criterion
		for (EncounterCriterion criterion : encounterCriteria) {
			String criterionEcl = getGivenOrSubsetEcl(criterion);
			if (criterionEcl != null) {
				List<String> conceptIds = eclToConceptsMap.get(criterionEcl);
				BoolQueryBuilder encounterQuery = boolQuery();
				encounterQuery.must(termsQuery(Patient.Fields.encounters + "." + ClinicalEncounter.Fields.CONCEPT_ID, conceptIds));
				if (criterion.getMinDate() != null || criterion.getMaxDate() != null) {
					// Setting this date range is not enough to filter the patient encounters because all encounters and all dates
					// are stored together in the index of the patient document.
					// Additional filtering happens in the Painless script.
					// Setting this range at the index level reduces the number of documents the painless script needs to process.
					RangeQueryBuilder rangeQuery = rangeQuery(Patient.Fields.encounters + "." + ClinicalEncounter.Fields.DATE_LONG);
					if (criterion.getMinDate() != null) {
						rangeQuery.gte(criterion.getMinDate().getTime());
					}
					if (criterion.getMaxDate() != null) {
						rangeQuery.lt(criterion.getMaxDate().getTime());
					}
					encounterQuery.must(rangeQuery);
				}
				if (criterion.isHas()) {
					patientFilter.must(encounterQuery);
				} else {
					patientFilter.mustNot(encounterQuery);
				}
			}
		}

		if (encounterCriteria.stream().anyMatch(EncounterCriterion::hasTimeConstraint)
				|| encounterCriteria.stream().anyMatch(EncounterCriterion::hasFrequency)) {

			// Convert parameter objects to simple types to be used in Elasticsearch Painless script which executes within a node.
			List<Map<String, Object>> encounterCriteriaMaps = encounterCriteria.stream().map(criterion -> {
				Map<String, Object> criterionMap = new HashMap<>();
				criterionMap.put("has", criterion.isHas());
				criterionMap.put("conceptECL", criterion.getConceptECL());
				criterionMap.put("minDate", criterion.getMinDate() != null ? criterion.getMinDate().getTime() : null);
				criterionMap.put("maxDate", criterion.getMaxDate() != null ? criterion.getMaxDate().getTime() : null);
				criterionMap.put("withinDaysAfterPreviouslyMatchedEncounter", criterion.getWithinDaysAfterPreviouslyMatchedEncounter());
				criterionMap.put("withinDaysBeforePreviouslyMatchedEncounter", criterion.getWithinDaysBeforePreviouslyMatchedEncounter());
				Frequency frequency = criterion.getFrequency();
				if (frequency != null) {
					Map<String, Object> frequencyMap = new HashMap<>();
					frequencyMap.put("minRepetitions", frequency.getMinRepetitions());
					frequencyMap.put("minTimeBetween", frequency.getMinTimeBetween());
					frequencyMap.put("maxTimeBetween", frequency.getMaxTimeBetween());
					frequencyMap.put("timeUnit", frequency.getTimeUnit());
					frequencyMap.put("timeUnitMillis", frequency.getTimeUnit() != null ? frequency.getTimeUnit().getMilliseconds() : 0);
					criterionMap.put("frequency", frequencyMap);
				}
				return criterionMap;
			}).collect(Collectors.toList());

			int dayInMillis = 1000 * 60 * 60 * 24;
			Map<String, Object> params = new HashMap<>();
			params.put("dayInMillis", dayInMillis);
			params.put("criterionMapsList", encounterCriteriaMaps);
			params.put("eclToConceptsMap", eclToConceptsMap);

			/*
				The following script is written in the Elasticsearch Painless script which is a Java like language.
				This runs on each Elasticsearch node as a map-reduce function.
				The script is purposefully basic using a minimum set of core Java classes.
				The following Java features do not seem to work in the Painless language: type autoboxing, recursive methods, incrementing variables, calling a function and returning the value on one line.
			 */
			patientFilter.filter(scriptQuery(new Script(ScriptType.INLINE, "painless", "" +
					// Util method
					"long getRelativeDate(long baseDate, Integer days, int multiplier, def params) {" +
					"	if (days != null) {" +
					"		def daysInt = days.intValue();" +
					"		if (daysInt == -1) {" + // -1 days means unlimited time
					"			daysInt = 365 * 200;" + // 200 years seems enough
					"		}" +
					"		long plusTime = ((long)daysInt * multiplier) * ((long)params.dayInMillis);" +
					"		return (long) baseDate + plusTime;" +
					"	}" +
					"	return 0;" +
					"}" +
					// Util method
					"boolean frequencyMatch(def doc, Map criterionMap, List criterionConceptIds) {" +
					"	Map frequencyMap = criterionMap.get('frequency');" +
					"	if (frequencyMap == null) {" +
					"		return true;" +
					"	}" +
					"	Integer minRepetitions = frequencyMap.get('minRepetitions').intValue();" +
					"	Integer minTimeBetween = frequencyMap.get('minTimeBetween');" +
					"	Integer maxTimeBetween = frequencyMap.get('maxTimeBetween');" +
					"	long timeUnitMillis = frequencyMap.get('timeUnitMillis');" +

						// Find all encounters for this patient with a matching conceptId
					"	List dates = new ArrayList();" +
					"	for (String conceptDateString : doc['encounters.conceptDate.keyword']) {" +
					"		int commaIndex = conceptDateString.indexOf(',');" +
					"		String candidateEncounterConceptId = conceptDateString.substring(0, commaIndex);" +
					"		long candidateEncounterDate = Long.parseLong(conceptDateString.substring(commaIndex + 1));" +
					"		if (criterionConceptIds.contains(candidateEncounterConceptId)) {" +
					"			dates.add(candidateEncounterDate);" +
					"		}" +
					"	}" +
					"" +
					"	if (dates.size().intValue() < minRepetitions.intValue()) {" +
							// Not enough matching encounters
					"		return false;" +
					"	}" +
					"	if (minTimeBetween == null && maxTimeBetween == null) {" +
							// Enough matching encounters and no time constraints
					"		return true;" +
					"	}" +
					"" +
						// Apply frequency time constraints
					"	dates.sort(null);" +
					"	long relativeEncounterTime = dates.remove(0);" +
					"	int repetitionsFound = 1;" +
					"	for (long nextEncounterTime : dates) {" +
					"		if (minTimeBetween != null) {" +
					"			long minTime = relativeEncounterTime + (minTimeBetween.intValue() * timeUnitMillis);" +
					"			if (nextEncounterTime < minTime) {" +
//					"				Debug.explain('nextEncounterTime < minTime, nextEncounterTime:' + nextEncounterTime + ', minTime:' + minTime);" +
					"				return false;" +
					"			}" +
//					"			Debug.explain('nextEncounterTime >= minTime, relativeEncounterTime:' + relativeEncounterTime + ', nextEncounterTime:' + nextEncounterTime + ', minTime:' + minTime);" +
					"		}" +
					"		if (maxTimeBetween != null) {" +
					"			long maxTime = relativeEncounterTime + (maxTimeBetween.intValue() * timeUnitMillis);" +
					"			if (nextEncounterTime > maxTime) {" +
//					"				Debug.explain('nextEncounterTime > maxTime, nextEncounterTime:' + nextEncounterTime + ', maxTime:' + maxTime);" +
					"				return false;" +
					"			}" +
					"		}" +
							// The time between relativeEncounterTime and nextEncounterTime is valid
					"		repetitionsFound = repetitionsFound + 1;" +
					"		if (minRepetitions != null && repetitionsFound == minRepetitions.intValue()) {" +
					"			return true;" +
					"		}" +
							// Make nextEncounterTime the new relativeEncounterTime and go round again
					"		relativeEncounterTime = nextEncounterTime;" +
					"	}" +
//					"	Debug.explain('No frequency match, dates:' + dates + ', criterionConceptIds:' + criterionConceptIds);" +
					"	return false;" +
					"}" +

					// Start of main method
					"List criterionMapsList = params.criterionMapsList;" +
					"if (criterionMapsList.isEmpty()) {" +
					"	return true;" +
					"}\n" +
//					"Debug.explain(criterionMapsList);" +
//					"Debug.explain(doc['encounters.conceptDate.keyword']);" +

					"boolean match = false;" +
					"long baseEncounterDate = 0;" +
					// Iterate each criterion to validate the encounters for this patient document
					"for (def criterionMap : criterionMapsList) {" +
//					"	Debug.explain('criterionMapsList:' + criterionMapsList);" +
					"	List criterionConceptIds = params.eclToConceptsMap.get(criterionMap.get('conceptECL'));" +
					"" +
					"	long minDate = -1;" +
					"	long maxDate = -1;" +
					"	if (criterionMap.get('minDate') != null) {" +
					"		minDate = criterionMap.get('minDate');" +
					"	}" +
					"	if (criterionMap.get('maxDate') != null) {" +
					"		maxDate = criterionMap.get('maxDate');" +
					"	}" +

					"" +
						// If forward/back cut-off days set calculate relative dates
					"	long minEncounterDate = 0;" +
					"	long maxEncounterDate = 0;" +
					"	Integer daysBeforePrevious = criterionMap.get('withinDaysBeforePreviouslyMatchedEncounter');" +
					"	Integer daysAfterPrevious = criterionMap.get('withinDaysAfterPreviouslyMatchedEncounter');" +
//					"	Debug.explain('daysBeforePrevious:' + daysBeforePrevious + ', daysAfterPrevious:' + daysAfterPrevious);" +

					"	if (baseEncounterDate != 0 && (daysBeforePrevious != null || daysAfterPrevious != null)) {" +
					"		minEncounterDate = getRelativeDate(baseEncounterDate, daysBeforePrevious, -1, params);" +
					"		maxEncounterDate = getRelativeDate(baseEncounterDate, daysAfterPrevious, 1, params);" +
//					"		Debug.explain('baseEncounterDate:' + baseEncounterDate + ', daysBeforePrevious:' + daysBeforePrevious + ', minEncounterDate:' + minEncounterDate + " +
//					"				', daysAfterPrevious:' + daysAfterPrevious + ', maxEncounterDate:' + maxEncounterDate);" +
					"	}" +
					"" +

						// Iterate patient's encounters
					"	boolean encounterMatchFound = false;" +
					"	for (String conceptDateString : doc['encounters.conceptDate.keyword']) {" +
					"		if (encounterMatchFound == false) {" +
					"			int commaIndex = conceptDateString.indexOf(',');" +
					"			String encounterConceptId = conceptDateString.substring(0, commaIndex);" +
					"			long encounterDate = Long.parseLong(conceptDateString.substring(commaIndex + 1));" +
					"			if (criterionConceptIds.contains(encounterConceptId)) {" +
					"				if ((minDate == -1 || encounterDate >= minDate)" +
					"					&& (maxDate == -1 || encounterDate < maxDate)" +
					"					&& (minEncounterDate == 0 || encounterDate >= minEncounterDate)" +
					"					&& (maxEncounterDate == 0 || encounterDate <= maxEncounterDate)) {" +

					"					if (!criterionMap.get('has')) {" +
											// Criterion clauses match but criterion is negated
//					"						Debug.explain('Criterion clauses match but criterion is negated e:' + e + ', baseEncounterDate:' + baseEncounterDate);" +// TODO remove
					"						return false;" +
					"					}" +
					"" +
										// This encounter matches so far, frequency check next
					"					boolean frequencyMatch = frequencyMatch(doc, criterionMap, criterionConceptIds);" +
					"					if (frequencyMatch == false) {" +
//					"						Debug.explain('frequencyMatch: false');"
					"						return false;" +
					"					}" +

										// This criterion positive match
					"					baseEncounterDate = encounterDate;" +
					"					encounterMatchFound = true;" +
					"				}" +
					"			}" +
					"		}" +
					"	}" +
					"" +
					"	if (encounterMatchFound == false && criterionMap.get('has')) {" +
							// If we got to this point none of the encounters matched the current criterion clauses.
					"		return false;" +
					"	}" +
					"}" +
					"return true;",
					params)));
			}
		return patientFilter;
	}

	private String getGivenOrSubsetEcl(EncounterCriterion criterion) throws ServiceException {
		if (criterion != null) {
			String subsetId = criterion.getConceptSubsetId();
			if (!Strings.isNullOrEmpty(subsetId)) {
				Optional<Subset> subsetOptional = subsetRepository.findById(subsetId);
				if (!subsetOptional.isPresent()) {
					throw new ServiceException("Referenced subset does not exist. ROLE_ID:" + subsetId);
				}
				return subsetOptional.get().getEcl();
			}
			String conceptECL = criterion.getConceptECL();
			criterion.setConceptECL(conceptECL);
			return conceptECL;
		}
		return null;
	}

}
