package org.snomed.heathanalytics.query;

import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.store.ClinicalEncounterRepository;
import org.snomed.heathanalytics.store.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

@Service
public class QueryService {

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	public Page<ClinicalEncounter> fetchCohort(String conceptId) {
		return elasticsearchTemplate.queryForPage(new NativeSearchQueryBuilder()
						.withQuery(boolQuery().must(termQuery("conceptId", conceptId)))
						.withPageable(new PageRequest(1, 1000))
						.build(),
				ClinicalEncounter.class
		);
	}
}
