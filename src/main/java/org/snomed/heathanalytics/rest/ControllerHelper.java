package org.snomed.heathanalytics.rest;

import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;

public class ControllerHelper {

	static <T> org.springframework.data.domain.Page<T> aggregatedPageWorkaround(org.springframework.data.domain.Page<T> page) {
		if (page instanceof AggregatedPageImpl) {
			AggregatedPageImpl aggPage = (AggregatedPageImpl) page;
			return new PageImpl<>(page.getContent(), new PageRequest(aggPage.getNumber(), aggPage.getSize()), aggPage.getTotalElements());
		}
		return page;
	}

}
