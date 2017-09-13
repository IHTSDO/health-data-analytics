package org.snomed.heathanalytics.service;

import org.elasticsearch.common.Strings;
import org.ihtsdo.otf.sqs.service.SnomedQueryService;
import org.ihtsdo.otf.sqs.service.dto.ConceptResult;
import org.ihtsdo.otf.sqs.service.dto.ConceptResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.*;
import org.snomed.heathanalytics.pojo.Stats;
import org.snomed.heathanalytics.store.SubsetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.util.CloseableIterator;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

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

	public Page<Patient> fetchCohort(CohortCriteria cohortCriteria, int page, int size) throws ServiceException {
		Timer timer = new Timer();

		String primaryExposureECL = getCriterionEcl(cohortCriteria.getPrimaryExposure());
		RelativeCriterion inclusionCriterion = cohortCriteria.getInclusionCriteria();
		try {
			// Gather primary exposure concepts
			List<Long> primaryExposureConceptIds = getConceptIds(primaryExposureECL);
			timer.split("Gather primary exposure concepts");

			Map<String, Set<ClinicalEncounter>> inclusionRoleToEncounterMap = new HashMap<>();
			if (inclusionCriterion != null) {
				// Gather inclusion concepts
				List<Long> inclusionConceptIds = getConceptIds(getCriterionEcl(inclusionCriterion));
				timer.split("Gather inclusion concepts");

				// Gather inclusion encounters
				try (CloseableIterator<ClinicalEncounter> encounterStream = elasticsearchTemplate.stream(new NativeSearchQueryBuilder()
								.withFilter(termsQuery(ClinicalEncounter.Fields.CONCEPT_ID, inclusionConceptIds))
								.withPageable(LARGE_PAGE)
								.build(),
						ClinicalEncounter.class)) {
					encounterStream.forEachRemaining(e -> inclusionRoleToEncounterMap.computeIfAbsent(e.getRoleId(), s -> new HashSet<>()).add(e));
				}
				timer.split("Gather inclusion encounters");
			}

			// Identify patients matching all criteria
			final Set<String> matchingPatientIds = new HashSet<>();
			AtomicLong encounterCount = new AtomicLong(0L);
			NativeSearchQuery primaryExposureQuery = new NativeSearchQueryBuilder()
					.withFilter(termsQuery(ClinicalEncounter.Fields.CONCEPT_ID, primaryExposureConceptIds))
					.withPageable(LARGE_PAGE)
					.build();
			try (CloseableIterator<ClinicalEncounter> encounterStream = elasticsearchTemplate.stream(primaryExposureQuery, ClinicalEncounter.class)) {
				encounterStream.forEachRemaining(primaryExposure -> {
					if (inclusionCriterion == null || passesInclusionCriteria(primaryExposure, inclusionCriterion, inclusionRoleToEncounterMap)) {
						matchingPatientIds.add(primaryExposure.getRoleId());
						encounterCount.incrementAndGet();
					}
				});
			}
			inclusionRoleToEncounterMap.clear();
			timer.split("Identify patients matching all criteria");

			// Apply gender filter
			Gender genderFilter = cohortCriteria.getGender();
			if (genderFilter != null) {
				Set<String> newMatchingPatientIds = new HashSet<>();
				NativeSearchQuery patientsWithinDemographic = new NativeSearchQueryBuilder()
						.withQuery(termQuery(Patient.Fields.SEX, genderFilter.toString().toLowerCase()))
						.withFilter(termsQuery(Patient.Fields.ROLE_ID, matchingPatientIds))
						.withPageable(LARGE_PAGE)
						.build();
				try (CloseableIterator<Patient> patientStream = elasticsearchTemplate.stream(patientsWithinDemographic, Patient.class)) {
					patientStream.forEachRemaining(patient -> newMatchingPatientIds.add(patient.getRoleId()));
				}
				matchingPatientIds.clear();
				matchingPatientIds.addAll(newMatchingPatientIds);
				timer.split("Apply gender filter");
			}

			// Fetch page of patients
			PageRequest pageRequest = new PageRequest(page, size);
			NativeSearchQuery patientQuery = new NativeSearchQueryBuilder()
					.withFilter(termsQuery(Patient.Fields.ROLE_ID, matchingPatientIds))
					.withPageable(pageRequest)
					.build();

			// TODO: try switching back to withQuery and using an aggregation which gathers birth years as Integers
//			patientQuery.addAggregation(AggregationBuilders.dateHistogram("patient_birth_dates")
//					.field(Patient.FIELD_DOB).interval(DateHistogramInterval.YEAR));

			AggregatedPage<Patient> patientPage = elasticsearchTemplate.queryForPage(patientQuery, Patient.class);
			timer.split("Fetch page of patients");

			// Join patient encounters
			Map<String, Patient> patientPageMap = patientPage.getContent().stream().collect(Collectors.toMap(Patient::getRoleId, Function.identity()));
			try (CloseableIterator<ClinicalEncounter> patientEncounters = elasticsearchTemplate.stream(new NativeSearchQueryBuilder()
							// TODO: This should probably be a filter. Test with 1 million patients to compare performance.
							.withQuery(boolQuery().must(termsQuery(ClinicalEncounter.Fields.ROLE_ID, patientPageMap.keySet())))
							.withPageable(LARGE_PAGE)
							.build(),
					ClinicalEncounter.class)) {
				patientEncounters.forEachRemaining(encounter -> {
					Patient patient = patientPageMap.get(encounter.getRoleId());
					if (patient != null) {
						String conceptId = encounter.getConceptId().toString();
						if (primaryExposureConceptIds.contains(Long.parseLong(conceptId))) {
							// Mark this encounter as a primary exposure for the context of this cohort
							encounter.setPrimaryExposure(true);
						}
						encounter.setConceptTerm(conceptId);
						// Lookup FSN
						try {
							ConceptResult concept = snomedQueryService.retrieveConcept(conceptId);
							if (concept != null) {
								encounter.setConceptTerm(concept.getFsn());
							}
						} catch (org.ihtsdo.otf.sqs.service.exception.ServiceException e) {
							logger.warn("Failed to fetch concept term.", e);
						}
						patient.addEncounter(encounter);
					} else {
						logger.error("Patient missing from result map '{}'", encounter.getRoleId());
					}
				});
			}
			timer.split("Join patient encounters");

			logger.info("Fetched encounters for {} with {} results. Times {}",
					primaryExposureECL, encounterCount, timer.getTimes());

			return new PageImpl<>(patientPage.getContent(), pageRequest, patientPage.getTotalElements());
		} catch (org.ihtsdo.otf.sqs.service.exception.ServiceException e) {
			throw new ServiceException("Failed to process ECL query.", e);
		}
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

	private boolean passesInclusionCriteria(ClinicalEncounter primaryExposure, RelativeCriterion inclusionCriteria, Map<String, Set<ClinicalEncounter>> inclusionRoleToEncounterMap) {
		Date primaryExposureDate = primaryExposure.getDate();
		Set<ClinicalEncounter> clinicalEncounters = inclusionRoleToEncounterMap.get(primaryExposure.getRoleId());
		if (clinicalEncounters != null) {
			for (ClinicalEncounter clinicalEncounter : clinicalEncounters) {
				Integer includeDaysInPast = inclusionCriteria.getIncludeDaysInPast();
				if (includeDaysInPast != null) {
					GregorianCalendar lookBackCutOff = new GregorianCalendar();
					lookBackCutOff.setTime(primaryExposureDate);
					lookBackCutOff.add(Calendar.DAY_OF_YEAR, -includeDaysInPast);
					if (clinicalEncounter.getDate().after(lookBackCutOff.getTime())) {
						return true;
					}
				}
				Integer includeDaysInFuture = inclusionCriteria.getIncludeDaysInFuture();
				if (includeDaysInFuture != null) {
					GregorianCalendar lookForwardCutOff = new GregorianCalendar();
					lookForwardCutOff.setTime(primaryExposureDate);
					lookForwardCutOff.add(Calendar.DAY_OF_YEAR, includeDaysInFuture);
					if (clinicalEncounter.getDate().before(lookForwardCutOff.getTime())) {
						return true;
					}
				}

			}
		}
		return false;
	}

	private List<Long> getConceptIds(String ecl) throws org.ihtsdo.otf.sqs.service.exception.ServiceException {
		return snomedQueryService.eclQueryReturnConceptIdentifiers(ecl, 0, -1).getConceptIds();
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
		long clinicalEncounterCount = elasticsearchTemplate.count(searchQuery, ClinicalEncounter.class);
		return new Stats(new Date(), patientCount, clinicalEncounterCount);
	}

	public void setSnomedQueryService(SnomedQueryService snomedQueryService) {
		this.snomedQueryService = snomedQueryService;
	}
}
