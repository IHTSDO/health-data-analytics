package org.snomed.heathanalytics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.snomed.SnomedSubsumptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Set;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

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

		AggregatedPage<ClinicalEncounter> page = elasticsearchTemplate.queryForPage(new NativeSearchQueryBuilder()
						.withQuery(boolQuery()
								.should(termsQuery("conceptId", conceptId, descendantsOf))
						)
						.withPageable(new PageRequest(0, 100))
						.build(),
				ClinicalEncounter.class
		);
		logger.info("Fetched cohort for {} in {} milliseconds with {} results.",
				conceptId, new Date().getTime() - start, page.getTotalElements());
		return page;
	}
}
