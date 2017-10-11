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
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.util.CloseableIterator;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.elasticsearch.index.query.QueryBuilders.*;

@Service
public class QueryService {

	private static final PageRequest LARGE_PAGE = new PageRequest(0, 1000);

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Autowired
	private SnomedQueryService snomedQueryService;

	@Autowired
	private SubsetRepository subsetRepository;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public Page<Patient> fetchCohort(CohortCriteria cohortCriteria) throws ServiceException {
		return fetchCohort(cohortCriteria, 0, 100);
	}

	public Page<Patient> fetchCohort(CohortCriteria cohortCriteria, int page, int size) throws ServiceException {
		GregorianCalendar now = new GregorianCalendar();
		Timer timer = new Timer();

		BoolQueryBuilder patientQuery = boolQuery();

		if (cohortCriteria.getGender() != null) {
			patientQuery.must(termQuery(Patient.Fields.GENDER, cohortCriteria.getGender().toString().toLowerCase()));
		}
		Integer minAge = cohortCriteria.getMinAge();
		Integer maxAge = cohortCriteria.getMaxAge();
		if (minAge != null || maxAge != null) {
			// Crude match using birth year
			RangeQueryBuilder rangeQueryBuilder = rangeQuery(Patient.Fields.DOB_YEAR);
			int thisYear = now.get(Calendar.YEAR);
			if (minAge != null) {
				rangeQueryBuilder.lte(thisYear - minAge);
			}
			if (maxAge != null) {
				rangeQueryBuilder.gte(thisYear - maxAge);
			}
			patientQuery.must(rangeQueryBuilder);
		}

		BoolQueryBuilder patientFilter = boolQuery();

		// Fetch conceptIds of each criterion
		List<Criterion> criteria = new ArrayList<>();
		criteria.add(cohortCriteria.getPrimaryExposure());
		criteria.addAll(cohortCriteria.getInclusionCriteria());
		Map<Criterion, List<Long>> criterionToConceptIdMap = new HashMap<>();
		for (Criterion criterion : criteria) {
			String criterionEcl = getCriterionEcl(criterion);
			timer.split("Fetching concepts for ECL " + criterionEcl);
			List<Long> conceptIds = getConceptIds(criterionEcl);
			patientFilter.must(termsQuery(Patient.Fields.encounters + "." + ClinicalEncounter.Fields.CONCEPT_ID, conceptIds));
			criterionToConceptIdMap.put(criterion, conceptIds);
		}

		List<Patient> patientPage = new ArrayList<>();
		AtomicInteger patientCount = new AtomicInteger();
		int offset = page * size;
		int limit = offset + size;
		Map<Long, TermHolder> conceptTerms = new Long2ObjectOpenHashMap<>();
		try (CloseableIterator<Patient> patientStream = elasticsearchTemplate.stream(new NativeSearchQueryBuilder()
						.withQuery(patientQuery)
						.withFilter(patientFilter)
						.withPageable(LARGE_PAGE)
						.build(),
				Patient.class)) {
			patientStream.forEachRemaining(patient -> {
				if (doEncounterDatesMatchCriteria(patient.getEncounters(), cohortCriteria.getPrimaryExposure(), cohortCriteria.getInclusionCriteria(), criterionToConceptIdMap)) {
					long number = patientCount.incrementAndGet();
					if (number > offset && number <= limit) {
						for (ClinicalEncounter encounter : patient.getEncounters()) {
							// Assign a TermHolder to avoid opening a try block within this loop
							encounter.setConceptTerm(conceptTerms.computeIfAbsent(encounter.getConceptId(), conceptId -> new TermHolder()));
						}
						patientPage.add(patient);
					}
				}
			});
		}
		timer.split("Fetching patients");

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

		logger.info("Times: {}", timer.getTimes());

		// TODO: try switching back to withQuery and using an aggregation which gathers birth years as Integers
//			patientQuery.addAggregation(AggregationBuilders.dateHistogram("patient_birth_dates")
//					.field(Patient.FIELD_DOB).interval(DateHistogramInterval.YEAR));

		return new PageImpl<>(patientPage, new PageRequest(page, size), patientCount.get());
	}

	// Given set of encounters
	// Find encounters which match primary cri
	// For each of these find encounters which match relative cri 1
	// For each of these find encounters which match relative cri 2
	// cont
	// if any found return true, else false

	private boolean doEncounterDatesMatchCriteria(Set<ClinicalEncounter> allEncounters, Criterion primaryCriterion, List<RelativeCriterion> relativeCriteria, Map<Criterion, List<Long>> criterionToConceptIdMap) {
		for (ClinicalEncounter encounter : allEncounters) {
			if (criterionToConceptIdMap.get(primaryCriterion).contains(encounter.getConceptId())) {
				if (relativeCriteria.isEmpty() || recursiveEncounterMatch(encounter, getCriteriaStack(relativeCriteria), allEncounters, criterionToConceptIdMap)) {
					encounter.setPrimaryExposure(true);
					return true;
				}
			}
		}
		return false;
	}

	private Stack<RelativeCriterion> getCriteriaStack(List<RelativeCriterion> relativeCriteria) {
		Stack<RelativeCriterion> criterionStack = new Stack<>();
		criterionStack.addAll(relativeCriteria);
		return criterionStack;
	}

	private boolean recursiveEncounterMatch(ClinicalEncounter baseEncounter, Stack<RelativeCriterion> criterionStack, Set<ClinicalEncounter> allEncounters, Map<Criterion, List<Long>> criterionToConceptIdMap) {
		RelativeCriterion criterion = criterionStack.pop();

		List<Long> conceptIds = criterionToConceptIdMap.get(criterion);
		Date lookBackCutOffDate = getRelativeDate(baseEncounter.getDate(), criterion.getIncludeDaysInPast(), -1);
		Date lookForwardCutOffDate = getRelativeDate(baseEncounter.getDate(), criterion.getIncludeDaysInFuture(), 1);

		for (ClinicalEncounter encounter : allEncounters) {
			if (conceptIds.contains(encounter.getConceptId()) &&
					((lookBackCutOffDate != null && encounter.getDate().after(lookBackCutOffDate)) ||
							(lookForwardCutOffDate != null && encounter.getDate().before(lookForwardCutOffDate)))) {
				return criterionStack.isEmpty() || recursiveEncounterMatch(encounter, criterionStack, allEncounters, criterionToConceptIdMap);
			}
		}

		return false;
	}

	private Date getRelativeDate(Date baseDate, Integer days, int multiplier) {
		// TODO: This could be optimised using a millisecond calculation rather than calendar
		if (days != null) {
			GregorianCalendar lookBackCutOff = new GregorianCalendar();
			lookBackCutOff.setTime(baseDate);
			lookBackCutOff.add(Calendar.DAY_OF_YEAR, days * multiplier);
			return lookBackCutOff.getTime();
		}
		return null;
	}


	private String getCriterionEcl(Criterion criterion) throws ServiceException {
		if (criterion != null) {
			String subsetId = criterion.getSubsetId();
			if (!Strings.isNullOrEmpty(subsetId)) {
				Subset subset = subsetRepository.findOne(subsetId);
				if (subset == null) {
					throw new ServiceException("Referenced subset does not exist. ROLE_ID:" + subsetId);
				}
				return subset.getEcl();
			}
			return criterion.getEcl();
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
