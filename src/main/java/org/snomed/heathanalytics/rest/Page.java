package org.snomed.heathanalytics.rest;

import java.util.List;

public class Page<T> {

	private final org.springframework.data.domain.Page<T> page;

	public Page(org.springframework.data.domain.Page<T> page) {
		this.page = page;
	}

	public List<T> getContent() {
		return page.getContent();
	}

	public int getNumber() {
		return page.getNumber();
	}

	public int getSize() {
		return page.getSize();
	}

	public int getTotalPages() {
		return page.getTotalPages();
	}

	public long getTotalElements() {
		return page.getTotalElements();
	}
}
