package org.snomed.heathanalytics.server.service;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.ihtsdo.otf.sqs.service.dto.ConceptResult;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.elasticsearch.index.query.QueryBuilders.*;

@Service
public class QueryService {

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
		SearchQuery searchQuery = new NativeSearchQueryBuilder().build();
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
		Map<String, List<Long>> eclToConceptsMap = new HashMap<>();
		for (EncounterCriterion criterion : encounterCriteria) {
			String criterionEcl = getGivenOrSubsetEcl(criterion);
			if (criterionEcl != null) {
				if (!eclToConceptsMap.containsKey(criterionEcl)) {
					timer.split("Fetching concepts for ECL " + criterionEcl);
					eclToConceptsMap.put(criterionEcl, snomedService.getConceptIds(criterionEcl));
				}
			}
		}

		BoolQueryBuilder patientEncounterFilter = getPatientEncounterFilter(encounterCriteria, eclToConceptsMap);

		Map<Long, TermHolder> conceptTerms = new Long2ObjectOpenHashMap<>();
		PageRequest pageable = PageRequest.of(page, size);
		NativeSearchQueryBuilder patientElasticQuery = new NativeSearchQueryBuilder()
				.withQuery(patientQuery)
				.withFilter(patientEncounterFilter)
				.withPageable(pageable);

		List<EncounterCriterion> encounterCriteriaWithCPTAnalysis = encounterCriteria.stream().filter(EncounterCriterion::isIncludeCPTAnalysis).collect(Collectors.toList());
		if (!encounterCriteriaWithCPTAnalysis.isEmpty()) {
			Set<Long> allConcepts = new HashSet<>();
			for (EncounterCriterion criterion : encounterCriteriaWithCPTAnalysis) {
				List<Long> concepts = eclToConceptsMap.get(criterion.getConceptECL());
				allConcepts.addAll(concepts);
			}
			long[] conceptsToInclude = new long[allConcepts.size()];
			int a = 0;
			for (Long conceptId : allConcepts) {
				conceptsToInclude[a] = conceptId;
				a++;
			}
			patientElasticQuery.addAggregation(
					AggregationBuilders.terms("encounterConceptCounts")
							.field(Patient.Fields.encounters + "." + ClinicalEncounter.Fields.CONCEPT_ID)
							.includeExclude(new IncludeExclude(conceptsToInclude, new long[] {})));
		}

		// Grab page of Patients from Elasticsearch.
		Page<Patient> patients = elasticsearchTemplate.queryForPage(patientElasticQuery.build(), Patient.class);
		timer.split("Fetching patients");
		if (!encounterCriteriaWithCPTAnalysis.isEmpty()) {
			AggregatedPage<Patient> aggregatedPage = (AggregatedPage<Patient>) patients;
			ParsedLongTerms encounterConceptCounts = (ParsedLongTerms) aggregatedPage.getAggregation("encounterConceptCounts");
			List<? extends Terms.Bucket> buckets = encounterConceptCounts.getBuckets();
			Map<String, CPTCode> snomedToCptMap = cptService.getSnomedToCptMap();
			Map<String, CPTTotals> cptTotalsMap = new HashMap<>();
			for (Terms.Bucket bucket : buckets) {
				String conceptId = bucket.getKeyAsString();
				CPTCode cptCode = snomedToCptMap.get(conceptId);
				if (cptCode != null) {
					cptTotalsMap.computeIfAbsent(cptCode.getCptCode(), (c) -> new CPTTotals(cptCode)).addCount(bucket.getDocCount());
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
		if (!conceptTerms.isEmpty()) {
			try {
				for (Long conceptId : conceptTerms.keySet()) {
					ConceptResult conceptResult = snomedService.findConcept(conceptId.toString());
					if (conceptResult != null) {
						conceptTerms.get(conceptId).setTerm(conceptResult.getFsn());
					}
				}
			} catch (ServiceException e) {
				logger.warn("Failed to retrieve concept terms", e);
			}
			timer.split("Fetching concept terms");
		}

		logger.info("Times: {}", timer.getTimes());
		return patients;
	}

	private void validateCriteria(CohortCriteria patientCriteria) {
		List<EncounterCriterion> encounterCriteria = patientCriteria.getEncounterCriteria();
		for (int i = 0; i < encounterCriteria.size(); i++) {
			EncounterCriterion encounterCriterion = encounterCriteria.get(i);
			if ((Strings.isNullOrEmpty(encounterCriterion.getConceptECL()) && Strings.isNullOrEmpty(encounterCriterion.getConceptSubsetId()))
					|| (!Strings.isNullOrEmpty(encounterCriterion.getConceptECL()) && !Strings.isNullOrEmpty(encounterCriterion.getConceptSubsetId()))) {
				throw new IllegalArgumentException(format("EncounterCriterion[%s] must have either conceptECL or conceptSubsetId.", i));
			}
			Frequency frequency = encounterCriterion.getFrequency();
			if (frequency != null) {
				if (!encounterCriterion.isHas()) {
					throw new IllegalArgumentException(format("EncounterCriterion[%s].frequency can only be used when has=true.", i));
				}
				if (frequency.getMinRepetitions() == null || frequency.getMinRepetitions() < 1) {
					throw new IllegalArgumentException(format("EncounterCriterion[%s].frequency.minRepetitions must be a positive integer greater than 1.", i));
				}
				if (frequency.getMinTimeBetween() != null && frequency.getMinTimeBetween() < 0) {
					throw new IllegalArgumentException(format("EncounterCriterion[%s].frequency.minTimeBetween must be a positive integer.", i));
				}
				if (frequency.getMaxTimeBetween() != null && frequency.getMaxTimeBetween() < 0) {
					throw new IllegalArgumentException(format("EncounterCriterion[%s].frequency.maxTimeBetween must be a positive integer.", i));
				}
				if (frequency.getMinTimeBetween() != null && frequency.getMaxTimeBetween() != null
						&& frequency.getMinTimeBetween() > frequency.getMaxTimeBetween()) {
					throw new IllegalArgumentException(format("EncounterCriterion[%s].frequency.minTimeBetween must be less than maxTimeBetween.", i));
				}
				if (frequency.getTimeUnit() == null) {
					throw new IllegalArgumentException(format("EncounterCriterion[%s].frequency.timeUnit is required.", i));
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

	private BoolQueryBuilder getPatientEncounterFilter(List<EncounterCriterion> encounterCriteria, Map<String, List<Long>> eclToConceptsMap) throws ServiceException {
		BoolQueryBuilder patientFilter = boolQuery();

		// Fetch conceptIds of each criterion
		for (EncounterCriterion criterion : encounterCriteria) {
			String criterionEcl = getGivenOrSubsetEcl(criterion);
			if (criterionEcl != null) {
				List<Long> conceptIds = eclToConceptsMap.get(criterionEcl);
				if (criterion.isHas()) {
					patientFilter.must(termsQuery(Patient.Fields.encounters + "." + ClinicalEncounter.Fields.CONCEPT_ID, conceptIds));
				} else {
					patientFilter.mustNot(termsQuery(Patient.Fields.encounters + "." + ClinicalEncounter.Fields.CONCEPT_ID, conceptIds));
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
				criterionMap.put("withinDaysAfterPreviouslyMatchedEncounter", criterion.getWithinDaysAfterPreviouslyMatchedEncounter());
				criterionMap.put("withinDaysBeforePreviouslyMatchedEncounter", criterion.getWithinDaysBeforePreviouslyMatchedEncounter());
				Frequency frequency = criterion.getFrequency();
				if (frequency != null) {
					Map<String, Object> frequencyMap = new HashMap<>();
					frequencyMap.put("minRepetitions", frequency.getMinRepetitions());
					frequencyMap.put("minTimeBetween", frequency.getMinTimeBetween());
					frequencyMap.put("maxTimeBetween", frequency.getMaxTimeBetween());
					frequencyMap.put("timeUnit", frequency.getTimeUnit());
					frequencyMap.put("timeUnitMillis", frequency.getTimeUnit().getMilliseconds());
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
				The script is purposefully basic using a minimum set of core Java classes.
				The following Java features do not seem to work in the Painless language: type autoboxing, recursive methods, incrementing variables, calling a function and returning the value on one line.
			 */
			patientFilter.filter(scriptQuery(new Script(ScriptType.INLINE, "painless", "" +
					// Util method
					"long getRelativeDate(Long baseDate, Integer days, int multiplier, def params) {" +
					"	if (days != null) {" +
					"		def daysInt = days.intValue();" +
					"		if (daysInt == -1) {" + // -1 days means unlimited time
					"			daysInt = 365 * 200;" + // 200 years seems enough
					"		}" +
					"		long plusTime = ((long)daysInt * multiplier) * ((long)params.dayInMillis);" +
					"		return (long) baseDate.longValue() + plusTime;" +
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
					"	for (int o = 0; o < doc['encounters.conceptId'].length; o++) {" +
					"		Long otherEncounterConceptId = doc['encounters.conceptId'][o];" +
					"		Long otherEncounterDate = doc['encounters.dateLong'][o];" +
					"		if (criterionConceptIds.contains(otherEncounterConceptId)) {" +
					"			dates.add(otherEncounterDate);" +
					"		}" +
					"	}" +
					"" +
					"	if (minTimeBetween == null && maxTimeBetween == null) {" +
					"		if (minRepetitions != null) {" +
								// No time constraints, just check repetition count
//					"			Debug.explain('Size check only');" +
					"			return dates.size().intValue() >= minRepetitions.intValue();" +
					"		} else {" +
					"			return true;" +
					"		}" +
					"	}" +
					"	if (dates.size() <= 1) {" +
					"		return false;" +
					"	}" +
					"" +
						// Apply frequency time constraints
					"	long relativeEncounterTime = dates.remove(0).longValue();" +
					"	int repetitionsFound = 1;" +
					"	for (int o = 0; 0 < dates.size(); o++) {" +
					"		long nextEncounterTime = dates.get(o).longValue();" +
					"		if (minTimeBetween != null) {" +
					"			long minTime = relativeEncounterTime + (minTimeBetween.intValue() * timeUnitMillis);" +
					"			if (nextEncounterTime < minTime) {" +
//					"				Debug.explain('nextEncounterTime < minTime, nextEncounterTime:' + nextEncounterTime + ', minTime:' + minTime);" +
					"				return false;" +
					"			}" +
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

					"boolean match = false;" +
					"Long baseEncounterDate = null;" +
					// Iterate each criterion to validate the encounters for this patient document
					"for (int e = 0; e < criterionMapsList.length; e++) {" +

					"	def criterionMap = criterionMapsList.get(e);" +
//					"	Debug.explain('Here ' + e + ', criterionMap:' + criterionMap);" +
					"	List criterionConceptIds = params.eclToConceptsMap.get(criterionMap.get('conceptECL'));" +
						// Force elements of criterionConceptIds to be Long. Elasticsearch converts number params to the smallest number type which we don't want.
					"	List forceLongList = new ArrayList();" +
					"	for (int l = 0; l < criterionConceptIds.size(); l++) {" +
					"		forceLongList.add((Long) criterionConceptIds.get(l));" +
					"	}" +
					"	criterionConceptIds = forceLongList;" +
					"" +
						// If forward/back cut-off days set calculate relative dates
					"	long minEncounterDate = 0;" +
					"	long maxEncounterDate = 0;" +
					"	Integer daysBeforePrevious = criterionMap.get('withinDaysBeforePreviouslyMatchedEncounter');" +
					"	Integer daysAfterPrevious = criterionMap.get('withinDaysAfterPreviouslyMatchedEncounter');" +

					"	if (baseEncounterDate != null && (daysBeforePrevious != null || daysAfterPrevious != null)) {" +
					"		minEncounterDate = getRelativeDate(baseEncounterDate, daysBeforePrevious, -1, params);" +
					"		maxEncounterDate = getRelativeDate(baseEncounterDate, daysAfterPrevious, 1, params);" +
//					"		Debug.explain('baseEncounterDate:' + baseEncounterDate + ', daysBeforePrevious:' + daysBeforePrevious + ', minEncounterDate:' + minEncounterDate + " +
//					"				', daysAfterPrevious:' + daysAfterPrevious + ', maxEncounterDate:' + maxEncounterDate);" +
					"	}" +
					"" +

						// Iterate patient's encounters
					"	boolean encounterMatchFound = false;" +
					"	for (int i = 0; encounterMatchFound == false && i < doc['encounters.conceptId'].length; i++) {" +
					"		Long encounterConceptId = doc['encounters.conceptId'][i];" +
					"		Long encounterDate = doc['encounters.dateLong'][i];" +
					"		if (criterionConceptIds.contains(encounterConceptId)) {" +

					"			if ((minEncounterDate == 0 || encounterDate.longValue() >= minEncounterDate)" +
					"					&& (maxEncounterDate == 0 || encounterDate.longValue() <= maxEncounterDate)) {" +

					"				if (!criterionMap.get('has')) {" +
										// Criterion clauses match but criterion is negated
					"					Debug.explain('Criterion clauses match but criterion is negated e:' + e + ', baseEncounterDate:' + baseEncounterDate);" +
					"					return false;" +
					"				}" +
					"" +
									// This encounter matches so far, frequency check next
					"				boolean frequencyMatch = frequencyMatch(doc, criterionMap, criterionConceptIds);" +
					"				if (frequencyMatch == false) {" +
					"					return false;" +
					"				}" +

									// This criterion positive match
					"				baseEncounterDate = encounterDate;" +
					"				encounterMatchFound = true;" +
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
