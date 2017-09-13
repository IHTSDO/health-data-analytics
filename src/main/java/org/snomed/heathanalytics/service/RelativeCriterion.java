package org.snomed.heathanalytics.service;

public class RelativeCriterion extends Criterion {

	// Optional
	private Integer includeDaysInPast;

	// Optional
	private Integer includeDaysInFuture;

	public Integer getIncludeDaysInPast() {
		return includeDaysInPast;
	}

	public void setIncludeDaysInPast(Integer includeDaysInPast) {
		this.includeDaysInPast = includeDaysInPast;
	}

	public Integer getIncludeDaysInFuture() {
		return includeDaysInFuture;
	}

	public void setIncludeDaysInFuture(Integer includeDaysInFuture) {
		this.includeDaysInFuture = includeDaysInFuture;
	}
}
