package org.snomed.heathanalytics.server.model;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class PageWithAggregations<T> extends PageImpl<T> {

	public PageWithAggregations(List<T> content, Pageable pageable, long total) {
		super(content, pageable, total);
	}

}
