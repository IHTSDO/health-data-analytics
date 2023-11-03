package org.snomed.heathanalytics.server.service;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
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
import org.snomed.heathanalytics.model.ClinicalEvent;
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
		List<EventCriterion> eventCriteria = patientCriteria.getEventCriteria();

		// Fetch conceptIds of each criterion
		Map<String, List<Long>> eclToConceptsMap = new HashMap<>();
		List<EventCriterion> allEventCriteria = new ArrayList<>(eventCriteria);
		List<CohortCriteria> exclusionCriteria = patientCriteria.getExclusionCriteria();
		exclusionCriteria.forEach(excludeCohort -> allEventCriteria.addAll(excludeCohort.getEventCriteria()));
		for (EventCriterion criterion : allEventCriteria) {
			String criterionEcl = getGivenOrSubsetEcl(criterion);
			if (criterionEcl != null) {
				if (!eclToConceptsMap.containsKey(criterionEcl)) {
					timer.split("Fetching concepts for ECL " + criterionEcl);
					eclToConceptsMap.put(criterionEcl, snomedService.getConceptIds(criterionEcl));
				}
			}
		}

		BoolQueryBuilder filterBoolBuilder = boolQuery();
		filterBoolBuilder.must(getPatientEventFilter(eventCriteria, eclToConceptsMap));
		for (CohortCriteria exclusionCriterion : exclusionCriteria) {
			BoolQueryBuilder exclusionBool = boolQuery();
			exclusionBool.must(getPatientClauses(exclusionCriterion.getGender(), exclusionCriterion.getMinAgeNow(), exclusionCriterion.getMaxAgeNow(), now));
			if (!exclusionCriterion.getEventCriteria().isEmpty()) {
				exclusionBool.must(getPatientEventFilter(exclusionCriterion.getEventCriteria(), eclToConceptsMap));
			}
			filterBoolBuilder.mustNot(exclusionBool);
		}
		patientQuery.filter(filterBoolBuilder);

		Map<Long, TermHolder> conceptTerms = new Long2ObjectOpenHashMap<>();
		PageRequest pageable = PageRequest.of(page, size);
		NativeSearchQueryBuilder patientElasticQuery = new NativeSearchQueryBuilder()
				.withQuery(patientQuery)
				.withPageable(pageable);

		List<EventCriterion> eventCriteriaWithCPTAnalysis = eventCriteria.stream().filter(EventCriterion::isIncludeCPTAnalysis).collect(Collectors.toList());
		if (!eventCriteriaWithCPTAnalysis.isEmpty()) {
			Set<Long> includeConcepts = new HashSet<>();
			for (EventCriterion criterion : eventCriteriaWithCPTAnalysis) {
				List<Long> concepts = eclToConceptsMap.get(criterion.getConceptECL());
				includeConcepts.addAll(concepts);
			}
			Map<String, Object> params = new HashMap<>();
			params.put("includeConcepts", includeConcepts);
			patientElasticQuery.addAggregation(
					AggregationBuilders.scriptedMetric("eventConceptCounts")
							.initScript(new Script(ScriptType.INLINE, "painless",
									"state.concepts = new HashMap();" +
									// Force elements of includeConcepts set to be of type Long.
									// Elasticsearch converts number params to the smallest number type which we don't want.
									"Set forceLongSet = new HashSet();" +
									"for (def includeConcept : params.includeConcepts) {" +
									"	forceLongSet.add((Long) includeConcept);" +
									"}" +
									"state.includeConcepts = forceLongSet;", params))
							.mapScript(new Script(
									"Map concepts = state.concepts;" +
									"for (Long conceptIdLong : doc['events.conceptId']) {" +
									"	if (state.includeConcepts.contains(conceptIdLong)) {" +
									"		String conceptId = conceptIdLong.toString();" +
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
		if (!eventCriteriaWithCPTAnalysis.isEmpty()) {

			Aggregations aggregations = searchHits.getAggregations();
			Map<String, Aggregation> stringAggregationMap = aggregations.asMap();
			ParsedScriptedMetric eventConceptCounts = (ParsedScriptedMetric) stringAggregationMap.get("eventConceptCounts");
			Object aggregation = eventConceptCounts.aggregation();
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
			if (patient.getEvents() == null) {
				patient.setEvents(Collections.emptySet());
			}
			patient.getEvents().forEach(event ->
					event.setConceptTerm(conceptTerms.computeIfAbsent(event.getConceptId(), conceptId -> new TermHolder())));
		});
		if (!conceptTerms.isEmpty()) {
			for (Long conceptId : conceptTerms.keySet()) {
				//catch every look up exception, otherwise no concept id will be found after first error
//				try {
//					conceptTerms.get(conceptId).setTerm(
//							snomedService.findConcept(conceptId.toString()).getFsn()
//					);
//				} catch (ServiceException e) {
//				}
			}
			timer.split("Fetching concept terms");
		}
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
		List<EventCriterion> eventCriteria = patientCriteria.getEventCriteria();
		for (int i = 0; i < eventCriteria.size(); i++) {
			EventCriterion eventCriterion = eventCriteria.get(i);
			if ((Strings.isNullOrEmpty(eventCriterion.getConceptECL()) && Strings.isNullOrEmpty(eventCriterion.getConceptSubsetId()))
					|| (!Strings.isNullOrEmpty(eventCriterion.getConceptECL()) && !Strings.isNullOrEmpty(eventCriterion.getConceptSubsetId()))) {
				throw new IllegalArgumentException(format("%sEventCriterion[%s] must have either conceptECL or conceptSubsetId.", prefix, i));
			}
			Frequency frequency = eventCriterion.getFrequency();
			if (frequency != null) {
				if (!eventCriterion.isHas()) {
					throw new IllegalArgumentException(format("%sEventCriterion[%s].frequency can only be used when has=true.", prefix, i));
				}
				if (frequency.getMinRepetitions() == null || frequency.getMinRepetitions() < 1) {
					throw new IllegalArgumentException(format("%sEventCriterion[%s].frequency.minRepetitions must be a positive integer greater than 1.", prefix, i));
				}
				if (frequency.getMinTimeBetween() != null && frequency.getMinTimeBetween() < 0) {
					throw new IllegalArgumentException(format("%sEventCriterion[%s].frequency.minTimeBetween must be a positive integer.", prefix, i));
				}
				if (frequency.getMaxTimeBetween() != null && frequency.getMaxTimeBetween() < 0) {
					throw new IllegalArgumentException(format("%sEventCriterion[%s].frequency.maxTimeBetween must be a positive integer.", prefix, i));
				}
				if (frequency.getMinTimeBetween() != null && frequency.getMaxTimeBetween() != null
						&& frequency.getMinTimeBetween() > frequency.getMaxTimeBetween()) {
					throw new IllegalArgumentException(format("%sEventCriterion[%s].frequency.minTimeBetween must be less than maxTimeBetween.", prefix, i));
				}
				if (frequency.getTimeUnit() == null && (frequency.getMinTimeBetween() != null || frequency.getMaxTimeBetween() != null)) {
					throw new IllegalArgumentException(format("%sEventCriterion[%s].frequency.timeUnit is required when minTimeBetween or maxTimeBetween is set.", prefix, i));
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

	private BoolQueryBuilder getPatientEventFilter(List<EventCriterion> eventCriteria, Map<String, List<Long>> eclToConceptsMap) throws ServiceException {
		BoolQueryBuilder patientFilter = boolQuery();

		// Fetch conceptIds of each criterion
		for (EventCriterion criterion : eventCriteria) {
			String criterionEcl = getGivenOrSubsetEcl(criterion);
			if (criterionEcl != null) {
				List<Long> conceptIds = eclToConceptsMap.get(criterionEcl);
				BoolQueryBuilder eventQuery = boolQuery();
				eventQuery.must(termsQuery(Patient.Fields.events + "." + ClinicalEvent.Fields.CONCEPT_ID, conceptIds));
				if (criterion.getMinDate() != null || criterion.getMaxDate() != null) {
					// Setting this date range is not enough to filter the patient events because all events and all dates
					// are stored together in the index of the patient document.
					// Additional filtering happens in the Painless script.
					// Setting this range at the index level reduces the number of documents the painless script needs to process.
					RangeQueryBuilder rangeQuery = rangeQuery(Patient.Fields.events + "." + ClinicalEvent.Fields.DATE_LONG);
					if (criterion.getMinDate() != null) {
						rangeQuery.gte(criterion.getMinDate().getTime());
					}
					if (criterion.getMaxDate() != null) {
						rangeQuery.lt(criterion.getMaxDate().getTime());
					}
					eventQuery.must(rangeQuery);
				}
				if (criterion.isHas()) {
					patientFilter.must(eventQuery);
				} else {
					patientFilter.mustNot(eventQuery);
				}
			}
		}

		if (eventCriteria.stream().anyMatch(EventCriterion::hasTimeConstraint)
				|| eventCriteria.stream().anyMatch(EventCriterion::hasFrequency)) {

			// Convert parameter objects to simple types to be used in Elasticsearch Painless script which executes within a node.
			List<Map<String, Object>> eventCriteriaMaps = eventCriteria.stream().map(criterion -> {
				Map<String, Object> criterionMap = new HashMap<>();
				criterionMap.put("has", criterion.isHas());
				criterionMap.put("conceptECL", criterion.getConceptECL());
				criterionMap.put("minDate", criterion.getMinDate() != null ? criterion.getMinDate().getTime() : null);
				criterionMap.put("maxDate", criterion.getMaxDate() != null ? criterion.getMaxDate().getTime() : null);
				criterionMap.put("withinDaysAfterPreviouslyMatchedEvent", criterion.getWithinDaysAfterPreviouslyMatchedEvent());
				criterionMap.put("withinDaysBeforePreviouslyMatchedEvent", criterion.getWithinDaysBeforePreviouslyMatchedEvent());
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
			params.put("criterionMapsList", eventCriteriaMaps);
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

						// Find all events for this patient with a matching conceptId
					"	List dates = new ArrayList();" +
					"	for (String conceptDateString : doc['events.conceptDate.keyword']) {" +
					"		int commaIndex = conceptDateString.indexOf(',');" +
					"		long candidateEventConceptId = Long.parseLong(conceptDateString.substring(0, commaIndex));" +
					"		long candidateEventDate = Long.parseLong(conceptDateString.substring(commaIndex + 1));" +
					"		if (criterionConceptIds.contains(candidateEventConceptId)) {" +
					"			dates.add(candidateEventDate);" +
					"		}" +
					"	}" +
					"" +
					"	if (dates.size().intValue() < minRepetitions.intValue()) {" +
							// Not enough matching events
					"		return false;" +
					"	}" +
					"	if (minTimeBetween == null && maxTimeBetween == null) {" +
							// Enough matching events and no time constraints
					"		return true;" +
					"	}" +
					"" +
						// Apply frequency time constraints
					"	dates.sort(null);" +
					"	long relativeEventTime = dates.remove(0);" +
					"	int repetitionsFound = 1;" +
					"	for (long nextEventTime : dates) {" +
					"		if (minTimeBetween != null) {" +
					"			long minTime = relativeEventTime + (minTimeBetween.intValue() * timeUnitMillis);" +
					"			if (nextEventTime < minTime) {" +
//					"				Debug.explain('nextEventTime < minTime, nextEventTime:' + nextEventTime + ', minTime:' + minTime);" +
					"				return false;" +
					"			}" +
//					"			Debug.explain('nextEventTime >= minTime, relativeEventTime:' + relativeEventTime + ', nextEventTime:' + nextEventTime + ', minTime:' + minTime);" +
					"		}" +
					"		if (maxTimeBetween != null) {" +
					"			long maxTime = relativeEventTime + (maxTimeBetween.intValue() * timeUnitMillis);" +
					"			if (nextEventTime > maxTime) {" +
//					"				Debug.explain('nextEventTime > maxTime, nextEventTime:' + nextEventTime + ', maxTime:' + maxTime);" +
					"				return false;" +
					"			}" +
					"		}" +
							// The time between relativeEventTime and nextEventTime is valid
					"		repetitionsFound = repetitionsFound + 1;" +
					"		if (minRepetitions != null && repetitionsFound == minRepetitions.intValue()) {" +
					"			return true;" +
					"		}" +
							// Make nextEventTime the new relativeEventTime and go round again
					"		relativeEventTime = nextEventTime;" +
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
//					"Debug.explain(doc['events.conceptDate.keyword']);" +

					"boolean match = false;" +
					"long baseEventDate = 0;" +
					// Iterate each criterion to validate the events for this patient document
					"for (def criterionMap : criterionMapsList) {" +
//					"	Debug.explain('criterionMapsList:' + criterionMapsList);" +
					"	List criterionConceptIds = params.eclToConceptsMap.get(criterionMap.get('conceptECL'));" +
						// Force elements of criterionConceptIds to be long. Elasticsearch converts number params to the smallest number type which we don't want.
					"	List forceLongList = new ArrayList();" +
					"	for (def criterionConceptId : criterionConceptIds) {" +
					"		forceLongList.add(((Long) criterionConceptId).longValue());" +
					"	}" +
					"	criterionConceptIds = forceLongList;" +
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
					"	long minEventDate = 0;" +
					"	long maxEventDate = 0;" +
					"	Integer daysBeforePrevious = criterionMap.get('withinDaysBeforePreviouslyMatchedEvent');" +
					"	Integer daysAfterPrevious = criterionMap.get('withinDaysAfterPreviouslyMatchedEvent');" +
//					"	Debug.explain('daysBeforePrevious:' + daysBeforePrevious + ', daysAfterPrevious:' + daysAfterPrevious);" +

					"	if (baseEventDate != 0 && (daysBeforePrevious != null || daysAfterPrevious != null)) {" +
					"		minEventDate = getRelativeDate(baseEventDate, daysBeforePrevious, -1, params);" +
					"		maxEventDate = getRelativeDate(baseEventDate, daysAfterPrevious, 1, params);" +
//					"		Debug.explain('baseEventDate:' + baseEventDate + ', daysBeforePrevious:' + daysBeforePrevious + ', minEventDate:' + minEventDate + " +
//					"				', daysAfterPrevious:' + daysAfterPrevious + ', maxEventDate:' + maxEventDate);" +
					"	}" +
					"" +

						// Iterate patient's events
					"	boolean eventMatchFound = false;" +
					"	for (String conceptDateString : doc['events.conceptDate.keyword']) {" +
					"		if (eventMatchFound == false) {" +
					"			int commaIndex = conceptDateString.indexOf(',');" +
					"			long eventConceptId = Long.parseLong(conceptDateString.substring(0, commaIndex));" +
					"			long eventDate = Long.parseLong(conceptDateString.substring(commaIndex + 1));" +
					"			if (criterionConceptIds.contains(eventConceptId)) {" +
					"				if ((minDate == -1 || eventDate >= minDate)" +
					"					&& (maxDate == -1 || eventDate < maxDate)" +
					"					&& (minEventDate == 0 || eventDate >= minEventDate)" +
					"					&& (maxEventDate == 0 || eventDate <= maxEventDate)) {" +

					"					if (!criterionMap.get('has')) {" +
											// Criterion clauses match but criterion is negated
//					"						Debug.explain('Criterion clauses match but criterion is negated e:' + e + ', baseEventDate:' + baseEventDate);" +// TODO remove
					"						return false;" +
					"					}" +
					"" +
										// This event matches so far, frequency check next
					"					boolean frequencyMatch = frequencyMatch(doc, criterionMap, criterionConceptIds);" +
					"					if (frequencyMatch == false) {" +
//					"						Debug.explain('frequencyMatch: false');"
					"						return false;" +
					"					}" +

										// This criterion positive match
					"					baseEventDate = eventDate;" +
					"					eventMatchFound = true;" +
					"				}" +
					"			}" +
					"		}" +
					"	}" +
					"" +
					"	if (eventMatchFound == false && criterionMap.get('has')) {" +
							// If we got to this point none of the events matched the current criterion clauses.
					"		return false;" +
					"	}" +
					"}" +
					"return true;",
					params)));
			}
		return patientFilter;
	}

	private String getGivenOrSubsetEcl(EventCriterion criterion) throws ServiceException {
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
