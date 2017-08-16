package org.snomed.heathanalytics.service;

public class InclusionCriteria {

	private String selectionECL;
	private Integer includeDaysInPast;
	private Integer includeDaysInFuture;

	public String getSelectionECL() {
		return selectionECL;
	}

	public void setSelectionECL(String selectionECL) {
		this.selectionECL = selectionECL;
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
}
