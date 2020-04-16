package org.snomed.heathanalytics.service;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.ihtsdo.otf.sqs.service.SnomedQueryService;
import org.ihtsdo.otf.sqs.service.dto.ConceptResult;
import org.ihtsdo.otf.sqs.service.dto.ConceptResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.*;
import org.snomed.heathanalytics.pojo.Stats;
import org.snomed.heathanalytics.pojo.TermHolder;
import org.snomed.heathanalytics.store.SubsetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.util.CloseableIterator;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.elasticsearch.index.query.QueryBuilders.*;

@Service
public class QueryService {

	public static final PageRequest LARGE_PAGE = new PageRequest(0, 1000);

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Autowired
	private SnomedQueryService snomedQueryService;

	@Autowired
	private SubsetRepository subsetRepository;

	private final SimpleDateFormat debugDateFormat = new SimpleDateFormat("yyyy/MM/dd");
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public Page<Patient> fetchCohort(CohortCriteria cohortCriteria) throws ServiceException {
		return fetchCohort(cohortCriteria, 0, 100);
	}

	public Page<Patient> fetchCohort(CohortCriteria cohortCriteria, int page, int size) throws ServiceException {
		return doFetchCohort(cohortCriteria, page, size, new GregorianCalendar(), new Timer());
	}

	private Page<Patient> doFetchCohort(CohortCriteria patientCriteria, int page, int size, GregorianCalendar now, Timer timer) throws ServiceException {
		BoolQueryBuilder patientQuery = getPatientClauses(patientCriteria.getGender(), patientCriteria.getMinAgeNow(), patientCriteria.getMaxAgeNow(), now);

		Map<EncounterCriterion, List<Long>> criterionToConceptIdMap = new HashMap<>();
		BoolQueryBuilder patientEncounterFilter = getPatientEncounterFilter(patientCriteria.getEncounterCriteria(), criterionToConceptIdMap, timer);

		Map<Long, TermHolder> conceptTerms = new Long2ObjectOpenHashMap<>();
		NativeSearchQueryBuilder patientElasticQuery = new NativeSearchQueryBuilder()
				.withQuery(patientQuery)
				.withFilter(patientEncounterFilter)
				.withPageable(LARGE_PAGE);

		Page<Patient> finalPatientPage;
		if (patientCriteria.getEncounterCriteria().size() > 1 && patientCriteria.getEncounterCriteria().stream().anyMatch(EncounterCriterion::hasTimeConstraint)) {
			// Stream through relevant Patients to apply relative date match in code.
			// Also do manual pagination
			List<Patient> patientList = new ArrayList<>();
			AtomicInteger patientCount = new AtomicInteger();
			int offset = page * size;
			int limit = offset + size;
			try (CloseableIterator<Patient> patientStream = elasticsearchTemplate.stream(patientElasticQuery.build(), Patient.class)) {
				patientStream.forEachRemaining(patient -> {
					if (checkEncounterDatesAndExclusions(patient.getEncounters(), patientCriteria.getEncounterCriteria(), criterionToConceptIdMap)) {
						long number = patientCount.incrementAndGet();
						if (number > offset && number <= limit) {
							patientList.add(patient);
						}
					}
				});
			}
			finalPatientPage = new PageImpl<>(patientList, new PageRequest(page, size > 0 ? size : 1), patientCount.get());
		} else {
			// Grab page of Patients from Elasticsearch.
			PageRequest pageRequest = new PageRequest(page, size);
			patientElasticQuery.withPageable(pageRequest);
			AggregatedPage<Patient> patients = elasticsearchTemplate.queryForPage(patientElasticQuery.build(), Patient.class);
			finalPatientPage = new PageImpl<>(patients.getContent(), pageRequest, patients.getTotalElements());
		}
		timer.split("Fetching patients");

		// Process matching patients for display
		finalPatientPage.getContent().forEach(patient -> {
			if (patient.getEncounters() == null) {
				patient.setEncounters(Collections.emptySet());
			}
			patient.getEncounters().forEach(encounter ->
					encounter.setConceptTerm(conceptTerms.computeIfAbsent(encounter.getConceptId(), conceptId -> new TermHolder())));
		});
		if (!conceptTerms.isEmpty()) {
			try {
				for (Long conceptId : conceptTerms.keySet()) {
					ConceptResult conceptResult = snomedQueryService.retrieveConcept(conceptId.toString());
					if (conceptResult != null) {
						conceptTerms.get(conceptId).setTerm(conceptResult.getFsn());
					}
				}
			} catch (org.ihtsdo.otf.sqs.service.exception.ServiceException e) {
				logger.warn("Failed to retrieve concept terms", e);
			}
			timer.split("Fetching concept terms");
		}

		logger.info("Times: {}", timer.getTimes());

		// TODO: try switching back to withQuery and using an aggregation which gathers birth years as Integers
//			patientQuery.addAggregation(AggregationBuilders.dateHistogram("patient_birth_dates")
//					.field(Patient.FIELD_DOB).interval(DateHistogramInterval.YEAR));

		return finalPatientPage;
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

	private BoolQueryBuilder getPatientEncounterFilter(List<EncounterCriterion> encounterCriteria, Map<EncounterCriterion, List<Long>> criterionToConceptIdMap, Timer timer) throws ServiceException {
		BoolQueryBuilder patientFilter = boolQuery();

		// Fetch conceptIds of each criterion
		for (EncounterCriterion criterion : encounterCriteria) {
			String criterionEcl = getCriterionEcl(criterion);
			if (criterionEcl != null) {
				timer.split("Fetching concepts for ECL " + criterionEcl);
				List<Long> conceptIds = getConceptIds(criterionEcl);
				if (criterion.isHas()) {
					patientFilter.must(termsQuery(Patient.Fields.encounters + "." + ClinicalEncounter.Fields.CONCEPT_ID, conceptIds));
				} else {
					patientFilter.mustNot(termsQuery(Patient.Fields.encounters + "." + ClinicalEncounter.Fields.CONCEPT_ID, conceptIds));
				}
				criterionToConceptIdMap.put(criterion, conceptIds);
			}
		}
		return patientFilter;
	}

	// Given set of encounterCriteria
	// Find encounters which match
	// For each of these find encounters which match relative cri 1
	// For each of these find encounters which match relative cri 2
	// cont
	// if any found return true, else false
	private boolean checkEncounterDatesAndExclusions(Set<ClinicalEncounter> allPatientEncounters, List<EncounterCriterion> encounterCriteria, Map<EncounterCriterion, List<Long>> criterionToConceptIdMap) {
		return recursiveEncounterMatch(null, allPatientEncounters, CollectionUtils.createStack(encounterCriteria), criterionToConceptIdMap);
	}

	// TODO: try converting this to an Elasticsearch 'painless' script which runs on the nodes of the cluster. https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-script-query.html
	private boolean recursiveEncounterMatch(ClinicalEncounter baseEncounter, Set<ClinicalEncounter> allEncounters, Stack<EncounterCriterion> criterionStack, Map<EncounterCriterion, List<Long>> criterionToConceptIdMap) {
		if (criterionStack.isEmpty()) {
			return true;
		}
		EncounterCriterion criterion = criterionStack.pop();

		List<Long> criterionConceptIds = criterionToConceptIdMap.get(criterion);
		Date lookBackCutOffDate = null;
		Date lookForwardCutOffDate = null;
		if (baseEncounter != null) {
			lookForwardCutOffDate = getRelativeDate(baseEncounter.getDate(), criterion.getWithinDaysAfterPreviouslyMatchedEncounter(), 1);
			lookBackCutOffDate = getRelativeDate(baseEncounter.getDate(), criterion.getWithinDaysBeforePreviouslyMatchedEncounter(), -1);
			logger.debug("{} baseEncounter date", debugDateFormat.format(baseEncounter.getDate()));
			logger.debug("{} lookForwardCutOffDate", lookForwardCutOffDate == null ? null : debugDateFormat.format(lookForwardCutOffDate));
			logger.debug("{} lookBackCutOffDate", lookBackCutOffDate == null ? null : debugDateFormat.format(lookBackCutOffDate));
		}

		for (ClinicalEncounter encounter : allEncounters) {
			if (criterionConceptIds.contains(encounter.getConceptId())) {
				if ((lookBackCutOffDate == null || encounter.getDate().equals(lookBackCutOffDate) || encounter.getDate().after(lookBackCutOffDate)) &&
						(lookForwardCutOffDate == null || encounter.getDate().equals(lookForwardCutOffDate) || encounter.getDate().before(lookForwardCutOffDate))) {
					if (!criterion.isHas()) {
						return false;
					}
					logger.debug("Encounter {} with date {} MATCH", encounter.getConceptId(), debugDateFormat.format(encounter.getDate()));
					return recursiveEncounterMatch(encounter, allEncounters, criterionStack, criterionToConceptIdMap);
				} else {
					logger.debug("Encounter {} with date {} NO match", encounter.getConceptId(), debugDateFormat.format(encounter.getDate()));
				}
			}
		}

		if (!criterion.isHas()) {
			return recursiveEncounterMatch(baseEncounter, allEncounters, criterionStack, criterionToConceptIdMap);
		}

		return false;
	}

	private Date getRelativeDate(Date baseDate, Integer days, int multiplier) {
		// TODO: This could be optimised using a millisecond calculation rather than calendar
		if (days != null) {
			GregorianCalendar lookBackCutOff = new GregorianCalendar();
			lookBackCutOff.setTime(baseDate);
			if (days == -1) {
				// This means an unlimited search during the patient's lifetime
				days = 365 * 200;
			}
			lookBackCutOff.add(Calendar.DAY_OF_YEAR, days * multiplier);
			return lookBackCutOff.getTime();
		}
		return null;
	}

	private String getCriterionEcl(EncounterCriterion criterion) throws ServiceException {
		if (criterion != null) {
			String subsetId = criterion.getConceptSubsetId();
			if (!Strings.isNullOrEmpty(subsetId)) {
				Subset subset = subsetRepository.findOne(subsetId);
				if (subset == null) {
					throw new ServiceException("Referenced subset does not exist. ROLE_ID:" + subsetId);
				}
				return subset.getEcl();
			}
			return criterion.getConceptECL();
		}
		return null;
	}

	private List<Long> getConceptIds(String ecl) throws ServiceException {
		try {
			return snomedQueryService.eclQueryReturnConceptIdentifiers(ecl, 0, -1).getConceptIds();
		} catch (org.ihtsdo.otf.sqs.service.exception.ServiceException e) {
			throw new ServiceException("Failed to process ECL query.", e);
		}
	}

	public ConceptResult findConcept(String conceptId) throws ServiceException {
		try {
			return snomedQueryService.retrieveConcept(conceptId);
		} catch (org.ihtsdo.otf.sqs.service.exception.ServiceException e) {
			throw new ServiceException("Failed to find concept by id '" + conceptId + "'", e);
		}
	}

	public ConceptResults findConcepts(String termPrefix, String ecQuery, int offset, int limit) throws ServiceException {
		try {
			return snomedQueryService.search(ecQuery, termPrefix, offset, limit);
		} catch (org.ihtsdo.otf.sqs.service.exception.ServiceException e) {
			throw new ServiceException("Failed to find concept by prefix '" + termPrefix + "'", e);
		}
	}

	public Stats getStats() {
		SearchQuery searchQuery = new NativeSearchQueryBuilder().build();
		long patientCount = elasticsearchTemplate.count(searchQuery, Patient.class);
		return new Stats(new Date(), patientCount);
	}

	public void setSnomedQueryService(SnomedQueryService snomedQueryService) {
		this.snomedQueryService = snomedQueryService;
	}
}
