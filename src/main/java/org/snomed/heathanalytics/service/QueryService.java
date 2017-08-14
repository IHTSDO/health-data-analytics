package org.snomed.heathanalytics.service;

import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.ihtsdo.otf.sqs.service.SnomedQueryService;
import org.ihtsdo.otf.sqs.service.dto.ConceptIdResults;
import org.ihtsdo.otf.sqs.service.dto.ConceptResult;
import org.ihtsdo.otf.sqs.service.dto.ConceptResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.pojo.Stats;
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
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

@Service
public class QueryService {

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Autowired
	private SnomedQueryService snomedQueryService;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public Page<Patient> fetchCohort(String ecl) throws ServiceException {
		Timer timer = new Timer();

		try {
			ConceptIdResults conceptResults = snomedQueryService.eclQueryReturnConceptIdentifiers(ecl, 0, 1000 * 1000);
			List<Long> conceptIds = conceptResults.getConceptIds();
			timer.split("Gather concepts");

			Set<String> totalRoleIds = new HashSet<>();
			AtomicLong encounterCount = new AtomicLong(0L);
			try (CloseableIterator<ClinicalEncounter> encounterStream = elasticsearchTemplate.stream(new NativeSearchQueryBuilder()
							.withFilter(termsQuery(ClinicalEncounter.FIELD_CONCEPT_ID, conceptIds))
							.withPageable(new PageRequest(0, 1000))
							.build(),
					ClinicalEncounter.class)) {
				encounterStream.forEachRemaining(e -> {
					totalRoleIds.add(e.getRoleId());
					encounterCount.incrementAndGet();
				});
			}
			timer.split("Fetch all encounters");

			PageRequest pageRequest = new PageRequest(0, 100);
			NativeSearchQuery patientQuery = new NativeSearchQueryBuilder()
					.withFilter(termsQuery(Patient.FIELD_ID, totalRoleIds))
					.withPageable(pageRequest)
					.build();
			// TODO: try switching back to withQuery and using an aggregation which gathers birth years as Integers
//			patientQuery.addAggregation(AggregationBuilders.dateHistogram("patient_birth_dates")
//					.field(Patient.FIELD_DOB).interval(DateHistogramInterval.YEAR));

			AggregatedPage<Patient> patientPage = elasticsearchTemplate.queryForPage(patientQuery, Patient.class);
			timer.split("Fetch page of patients");

			Map<String, Patient> patientPageMap = patientPage.getContent().stream().collect(Collectors.toMap(Patient::getRoleId, Function.identity()));
			try (CloseableIterator<ClinicalEncounter> patientEncounters = elasticsearchTemplate.stream(new NativeSearchQueryBuilder()
							.withQuery(boolQuery()
									.must(termsQuery(ClinicalEncounter.FIELD_ROLE_ID, patientPageMap.keySet()))
									.must(termsQuery(ClinicalEncounter.FIELD_CONCEPT_ID, conceptIds))
							)
							.withPageable(new PageRequest(0, 1000))
							.build(),
					ClinicalEncounter.class)) {
				patientEncounters.forEachRemaining(encounter -> {
					Patient patient = patientPageMap.get(encounter.getRoleId());
					if (patient != null) {
						String conceptId = encounter.getConceptId().toString();
						encounter.setConceptTerm(conceptId);
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
			timer.split("Fetch patient encounters");

			logger.info("Fetched encounters for {} with {} results. Times {}",
					ecl, encounterCount, timer.getTimes());

			return new PageImpl<>(patientPage.getContent(), pageRequest, patientPage.getTotalElements());
		} catch (org.ihtsdo.otf.sqs.service.exception.ServiceException e) {
			throw new ServiceException("Failed to process ECL query.", e);
		}
	}

	public ConceptResults findConcepts(String termPrefix, int offset, int limit) throws ServiceException {
		try {
			return snomedQueryService.search(null, termPrefix, offset, limit);
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
