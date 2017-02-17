package org.snomed.heathanalytics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.Date;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

@Service
public class QueryService {

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;
	private Logger logger = LoggerFactory.getLogger(getClass());

	public Page<ClinicalEncounter> fetchCohort(String conceptId) {
		long start = new Date().getTime();
		AggregatedPage<ClinicalEncounter> page = elasticsearchTemplate.queryForPage(new NativeSearchQueryBuilder()
						.withQuery(boolQuery()
								.should(termQuery("conceptId", conceptId))
								.should(termQuery("transitiveClosure", conceptId))
						)
						.withPageable(new PageRequest(1, 100))
						.build(),
				ClinicalEncounter.class
		);
		logger.info("Fetched cohort for {} in {} milliseconds with {} results.",
				conceptId, new Date().getTime() - start, page.getTotalElements());
		return page;
	}
}
