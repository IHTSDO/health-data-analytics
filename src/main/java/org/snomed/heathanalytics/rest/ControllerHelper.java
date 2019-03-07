package org.snomed.heathanalytics.rest;

import org.snomed.heathanalytics.service.NotFoundException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Optional;

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
			return new PageImpl<T>(page.getContent(), page.getPageable(), aggPage.getTotalElements());
		}
		return page;
	}

	static void throwIfNotFound(Optional optional, String type) {
		if (!optional.isPresent()) {
			throw new NotFoundException(type + " not found.");
		}
	}

}
