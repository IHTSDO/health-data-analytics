package org.snomed.heathanalytics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.pojo.Stats;
import org.snomed.heathanalytics.snomed.SnomedSubsumptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Set;

import static org.elasticsearch.index.query.QueryBuilders.*;

@Service
public class QueryService {

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Autowired
	private SnomedSubsumptionService snomedSubsumptionService;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public Page<ClinicalEncounter> fetchCohort(String conceptId) {
		long start = new Date().getTime();

		Set<Long> descendantsOf = snomedSubsumptionService.getDescendantsOf(conceptId);
		long gatherConcepts = new Date().getTime() - start;
		start = new Date().getTime();

		Page<ClinicalEncounter> page = elasticsearchTemplate.queryForPage(new NativeSearchQueryBuilder()
						.withQuery(boolQuery()
								.should(termsQuery("conceptId", conceptId, descendantsOf))
						)
						.withPageable(new PageRequest(0, 100))
						.build(),
				ClinicalEncounter.class
		);
		logger.info("Fetched cohort for {} with {} results. Gathering concepts took {} mills, Query took {} mills",
				conceptId, page.getTotalElements(), gatherConcepts, new Date().getTime() - start);
		return page;
	}

	public Stats getStats() {
		SearchQuery searchQuery = new NativeSearchQueryBuilder().build();
		long patientCount = elasticsearchTemplate.count(searchQuery, Patient.class);
		long clinicalEncounterCount = elasticsearchTemplate.count(searchQuery, ClinicalEncounter.class);
		return new Stats(new Date(), patientCount, clinicalEncounterCount);
	}
}
