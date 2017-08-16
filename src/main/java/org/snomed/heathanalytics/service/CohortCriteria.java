package org.snomed.heathanalytics.service;

public class CohortCriteria {

	private String primaryExposureECL;
	private InclusionCriteria inclusionCriteria;

	public CohortCriteria() {
	}

	public CohortCriteria(String primaryExposureECL) {
		this.primaryExposureECL = primaryExposureECL;
	}

	public String getPrimaryExposureECL() {
		return primaryExposureECL;
	}

	public void setPrimaryExposureECL(String primaryExposureECL) {
		this.primaryExposureECL = primaryExposureECL;
	}

	public InclusionCriteria getInclusionCriteria() {
		return inclusionCriteria;
	}

	public void setInclusionCriteria(InclusionCriteria inclusionCriteria) {
		this.inclusionCriteria = inclusionCriteria;
	}
}
