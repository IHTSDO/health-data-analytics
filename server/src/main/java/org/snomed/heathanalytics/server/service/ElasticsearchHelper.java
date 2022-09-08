package org.snomed.heathanalytics.server.service;

import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.List;
import java.util.stream.Collectors;

public class ElasticsearchHelper {

	public static <T> Page<T> queryForPage(Query query, Class<T> clazz, ElasticsearchOperations elasticsearchOperations) {
		SearchHits<T> searchHits = elasticsearchOperations.search(query, clazz);
		List<T> content = searchHits.stream().map(SearchHit::getContent).collect(Collectors.toList());
		Aggregations aggregations = searchHits.getAggregations();
		if (aggregations != null) {
//			aggregations.aggregations()
		}
//		PageWithAggregations
		return new PageImpl<T>(content, query.getPageable(), searchHits.getTotalHits());
	}

}
