package org.snomed.heathanalytics.server.rest;

import org.snomed.heathanalytics.server.service.NotFoundException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

class ControllerHelper {

	static ResponseEntity<Void> getCreatedResponse(String id) {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setLocation(ServletUriComponentsBuilder
				.fromCurrentRequest().path("/{id}")
				.buildAndExpand(id).toUri());
		return new ResponseEntity<>(httpHeaders, HttpStatus.CREATED);
	}

	static <T> org.springframework.data.domain.Page<T> aggregatedPageWorkaround(org.springframework.data.domain.Page<T> page) {
		if (page instanceof AggregatedPageImpl) {
			AggregatedPageImpl aggPage = (AggregatedPageImpl) page;
			return new PageImpl<>(page.getContent(), new PageRequest(aggPage.getNumber(), aggPage.getSize()), aggPage.getTotalElements());
		}
		return page;
	}

	static void throwIfNotFound(Object object, String type) {
		if (object == null) {
			throw new NotFoundException(type + " not found.");
		}
	}

}
