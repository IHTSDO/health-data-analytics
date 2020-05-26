package org.snomed.heathanalytics.server.service;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.util.CloseableIterator;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.*;

@Service
public class QueryService {

	public static final PageRequest LARGE_PAGE = PageRequest.of(0, 1000);

	@Autowired
	private SnomedService snomedService;

	@Autowired
	private ElasticsearchOperations elasticsearchTemplate;

	@Autowired
	private SubsetRepository subsetRepository;

	@Autowired
	private CPTService cptService;

	private final SimpleDateFormat debugDateFormat = new SimpleDateFormat("yyyy/MM/dd");
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

		Map<EncounterCriterion, List<Long>> criterionToConceptIdMap = new HashMap<>();
		List<EncounterCriterion> encounterCriteria = patientCriteria.getEncounterCriteria();
		BoolQueryBuilder patientEncounterFilter = getPatientEncounterFilter(encounterCriteria, criterionToConceptIdMap, timer);

		Map<Long, TermHolder> conceptTerms = new Long2ObjectOpenHashMap<>();
		NativeSearchQueryBuilder patientElasticQuery = new NativeSearchQueryBuilder()
				.withQuery(patientQuery)
				.withFilter(patientEncounterFilter)
				.withPageable(LARGE_PAGE);

		Page<Patient> finalPatientPage;
		boolean includeCptAnalysis = encounterCriteria.stream().anyMatch(EncounterCriterion::isIncludeCPTAnalysis);
		if (encounterCriteria.stream().anyMatch(EncounterCriterion::hasTimeConstraint)
				|| encounterCriteria.stream().anyMatch(EncounterCriterion::hasFrequency) || includeCptAnalysis) {
			// Stream through relevant Patients to apply relative date or frequency match in code.
			// Also do manual pagination
			List<Patient> patientList = new ArrayList<>();
			AtomicInteger patientCount = new AtomicInteger();
			int offset = page * size;
			int limit = offset + size;
			Map<Long, AtomicLong> encounterCountCollector = new HashMap<>();
			try (CloseableIterator<Patient> patientStream = elasticsearchTemplate.stream(patientElasticQuery.build(), Patient.class)) {
				patientStream.forEachRemaining(patient -> {
					if (checkEncounterDatesAndExclusions(patient.getEncounters(), encounterCriteria, criterionToConceptIdMap, encounterCountCollector)) {
						long number = patientCount.incrementAndGet();
						if (number > offset && number <= limit) {
							patientList.add(patient);
						}
					}
				});
			}

			// Add CPT information
			// Build map of CPT codes and counts where SNOMED CT encounters can be mapped to CPT
			// Many SNOMED CT can map to a single CPT code or to none.
			Map<String, CPTTotals> cptTotals = new HashMap<>();
			if (includeCptAnalysis) {
				Map<String, CPTCode> snomedToCptMap = cptService.getSnomedToCptMap();
				for (Long encounterConceptId : encounterCountCollector.keySet()) {
					CPTCode cptCode = snomedToCptMap.get(encounterConceptId.toString());
					if (cptCode != null) {
						cptTotals.computeIfAbsent(cptCode.getCptCode(), (i) -> new CPTTotals(cptCode)).addCount(encounterCountCollector.get(encounterConceptId).intValue());
					}
				}
				finalPatientPage = new PatientPageWithCPTTotals(patientList, PageRequest.of(page, size > 0 ? size : 1), patientCount.get(), cptTotals);
			} else {
				finalPatientPage = new PageImpl<>(patientList, PageRequest.of(page, size > 0 ? size : 1), patientCount.get());
			}
		} else {
			// Grab page of Patients from Elasticsearch.
			PageRequest pageRequest = PageRequest.of(page, size);
			patientElasticQuery.withPageable(pageRequest);
			Page<Patient> patients = elasticsearchTemplate.queryForPage(patientElasticQuery.build(), Patient.class);
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

		// TODO: try switching back to withQuery and using an aggregation which gathers birth years as Integers
//			patientQuery.addAggregation(AggregationBuilders.dateHistogram("patient_birth_dates")
//					.field(Patient.FIELD_DOB).interval(DateHistogramInterval.YEAR));

		return finalPatientPage;
	}

	private void validateCriteria(CohortCriteria patientCriteria) {
		List<EncounterCriterion> encounterCriteria = patientCriteria.getEncounterCriteria();
		for (int i = 0; i < encounterCriteria.size(); i++) {
			EncounterCriterion encounterCriterion = encounterCriteria.get(i);
			if ((Strings.isNullOrEmpty(encounterCriterion.getConceptECL()) && Strings.isNullOrEmpty(encounterCriterion.getConceptSubsetId()))
					|| (!Strings.isNullOrEmpty(encounterCriterion.getConceptECL()) && !Strings.isNullOrEmpty(encounterCriterion.getConceptSubsetId()))) {
				throw new IllegalArgumentException(String.format("EncounterCriterion[%s] must have either conceptECL or conceptSubsetId.", i));
			}
			Frequency frequency = encounterCriterion.getFrequency();
			if (frequency != null) {
				if (!encounterCriterion.isHas()) {
					throw new IllegalArgumentException(String.format("EncounterCriterion[%s].frequency can only be used when has=true.", i));
				}
				if (frequency.getMinRepetitions() == null || frequency.getMinRepetitions() < 1) {
					throw new IllegalArgumentException(String.format("EncounterCriterion[%s].frequency.minRepetitions must be a positive integer greater than 1.", i));
				}
				if (frequency.getMinTimeBetween() != null && frequency.getMinTimeBetween() < 0) {
					throw new IllegalArgumentException(String.format("EncounterCriterion[%s].frequency.minTimeBetween must be a positive integer.", i));
				}
				if (frequency.getMaxTimeBetween() != null && frequency.getMaxTimeBetween() < 0) {
					throw new IllegalArgumentException(String.format("EncounterCriterion[%s].frequency.minTimeBetween must be a positive integer.", i));
				}
				if (frequency.getTimeUnit() == null) {
					throw new IllegalArgumentException(String.format("EncounterCriterion[%s].frequency.timeUnit is required.", i));
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

	private BoolQueryBuilder getPatientEncounterFilter(List<EncounterCriterion> encounterCriteria, Map<EncounterCriterion, List<Long>> criterionToConceptIdMap, Timer timer) throws ServiceException {
		BoolQueryBuilder patientFilter = boolQuery();

		// Fetch conceptIds of each criterion
		for (EncounterCriterion criterion : encounterCriteria) {
			String criterionEcl = getCriterionEcl(criterion);
			if (criterionEcl != null) {
				timer.split("Fetching concepts for ECL " + criterionEcl);
				List<Long> conceptIds = snomedService.getConceptIds(criterionEcl);
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

	private boolean checkEncounterDatesAndExclusions(Set<ClinicalEncounter> allPatientEncounters, List<EncounterCriterion> encounterCriteria,
			Map<EncounterCriterion, List<Long>> criterionToConceptIdMap, Map<Long, AtomicLong> encounterCountCollector) {

		return recursiveEncounterMatch(null, allPatientEncounters, CollectionUtils.createStack(encounterCriteria), criterionToConceptIdMap, encounterCountCollector);
	}

	// TODO: try converting this to an Elasticsearch 'painless' script which runs on the nodes of the cluster.
	// https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-script-query.html
	private boolean recursiveEncounterMatch(ClinicalEncounter baseEncounter, Set<ClinicalEncounter> allEncounters, Stack<EncounterCriterion> criterionStack,
			Map<EncounterCriterion, List<Long>> criterionToConceptIdMap, Map<Long, AtomicLong> encounterCountCollector) {

		if (criterionStack.isEmpty()) {
			return true;
		}
		EncounterCriterion criterion = criterionStack.pop();

		List<Long> criterionConceptIds = criterionToConceptIdMap.get(criterion);

		// If forward/back cut-off days set calculate relative dates
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
						// Criterion clauses match but criterion is negated
						return false;
					}
					logger.debug("Encounter {} with date {} MATCH (pending any frequency check)", encounter.getConceptId(), debugDateFormat.format(encounter.getDate()));

					if (!frequencyMatch(allEncounters, criterion, criterionConceptIds)) {
						logger.debug("Frequency match FAILED");
						return false;
					}

					// This criterion positive match
					// Increment encounter count
					if (criterion.isIncludeCPTAnalysis()) {
						encounterCountCollector.computeIfAbsent(encounter.getConceptId(), (s) -> new AtomicLong()).incrementAndGet();
					}
					// Continue recursion
					return recursiveEncounterMatch(encounter, allEncounters, criterionStack, criterionToConceptIdMap, encounterCountCollector);
				} else {
					// Criterion does not match
					logger.debug("Encounter {} with date {} NO match", encounter.getConceptId(), debugDateFormat.format(encounter.getDate()));
				}
			}
		}

		if (!criterion.isHas()) {
			// If we got to this point the criterion clauses did not match and because the criterion is negated this is considered as a match. Continue recursion.
			return recursiveEncounterMatch(baseEncounter, allEncounters, criterionStack, criterionToConceptIdMap, encounterCountCollector);
		}

		return false;
	}

	private boolean frequencyMatch(Set<ClinicalEncounter> allEncounters, EncounterCriterion criterion, List<Long> criterionConceptIds) {
		Frequency frequency = criterion.getFrequency();
		if (frequency == null) {
			return true;
		}

		// Find other matching encounters sorted by date
		List<ClinicalEncounter> allMatchingEncounters = allEncounters.stream()
				.filter(encounter -> criterionConceptIds.contains(encounter.getConceptId()))
				.sorted(Comparator.comparing(ClinicalEncounter::getDate))
				.collect(Collectors.toList());
		logger.debug("Frequency match, allMatchingEncounters {}", allMatchingEncounters.size());

		if (frequency.getMinTimeBetween() == null && frequency.getMaxTimeBetween() == null) {
			// No time constraints, just check repetition count
			return allMatchingEncounters.size() >= frequency.getMinRepetitions();
		}

		// Apply frequency time constraints
		ClinicalEncounter relativeEncounter = allMatchingEncounters.remove(0);
		int repetitionsFound = 1;
		for (ClinicalEncounter nextEncounter : allMatchingEncounters) {
			// Validate time period between relativeEncounter and nextEncounter
			long time = relativeEncounter.getDate().getTime();
			long nextEncounterTime = nextEncounter.getDate().getTime();
			logger.debug("relativeEncounter time {}, nextEncounterTime {}", new Date(time), new Date(nextEncounterTime));
			if (frequency.getMinTimeBetween() != null) {
				long minTime = time + (frequency.getMinTimeBetween() * frequency.getTimeUnit().getMilliseconds());
				if (nextEncounterTime < minTime) {
					logger.debug("nextEncounterTime {} less than minTime {}", new Date(nextEncounterTime), new Date(minTime));
					return false;
				}
			}
			if (frequency.getMaxTimeBetween() != null) {
				long maxTime = time + (frequency.getMaxTimeBetween() * frequency.getTimeUnit().getMilliseconds());
				if (nextEncounterTime > maxTime) {
					logger.debug("nextEncounterTime {} greater than maxTime {}", new Date(nextEncounterTime), new Date(maxTime));
					return false;
				}
			}

			// The time between relativeEncounter and nextEncounter is valid
			repetitionsFound++;
			if (frequency.getMinRepetitions() != null && repetitionsFound == frequency.getMinRepetitions()) {
				// Enough repetitions have been found, the frequency criterion of this encounter for this patient has been met
				logger.debug("Enough repetitions found {}", repetitionsFound);
				return true;
			}
			// Make nextEncounter the new relativeEncounter and go round again
			relativeEncounter = nextEncounter;
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
				Optional<Subset> subsetOptional = subsetRepository.findById(subsetId);
				if (!subsetOptional.isPresent()) {
					throw new ServiceException("Referenced subset does not exist. ROLE_ID:" + subsetId);
				}
				return subsetOptional.get().getEcl();
			}
			return criterion.getConceptECL();
		}
		return null;
	}

}
