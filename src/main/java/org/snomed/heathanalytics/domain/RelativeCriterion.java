package org.snomed.heathanalytics.domain;

public class RelativeCriterion extends Criterion {

	// Optional
	private Integer includeDaysInPast;

	// Optional
	private Integer includeDaysInFuture;

	public RelativeCriterion() {
	}

	public RelativeCriterion(String ecl, Integer includeDaysInPast, Integer includeDaysInFuture) {
		super(ecl);
		this.includeDaysInPast = includeDaysInPast;
		this.includeDaysInFuture = includeDaysInFuture;
	}

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

	public boolean hasTimeConstraint() {
		return includeDaysInFuture != null || includeDaysInPast != null;
	}
}
