package org.snomed.heathanalytics.domain;

import org.snomed.heathanalytics.service.Criterion;
import org.snomed.heathanalytics.service.RelativeCriterion;

public class CohortCriteria {

	private Criterion primaryExposure;
	private RelativeCriterion inclusionCriteria;

	public CohortCriteria() {
	}

	public CohortCriteria(Criterion primaryExposure) {
		this.primaryExposure = primaryExposure;
	}

	public Criterion getPrimaryExposure() {
		return primaryExposure;
	}

	public void setPrimaryExposure(Criterion primaryExposure) {
		this.primaryExposure = primaryExposure;
	}

	public RelativeCriterion getInclusionCriteria() {
		return inclusionCriteria;
	}

	public void setInclusionCriteria(RelativeCriterion inclusionCriteria) {
		this.inclusionCriteria = inclusionCriteria;
	}
}
