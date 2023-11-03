package org.snomed.heathanalytics.server.model;

public class StatisticalCorrelationReportDefinition {

	private CohortCriteria baseCriteria;
	private EventCriterion treatmentCriterion;
	private EventCriterion negativeOutcomeCriterion;

	public StatisticalCorrelationReportDefinition() {
	}

	public StatisticalCorrelationReportDefinition(CohortCriteria baseCriteria, EventCriterion treatmentCriterion, EventCriterion negativeOutcomeCriterion) {
		this.baseCriteria = baseCriteria;
		this.treatmentCriterion = treatmentCriterion;
		this.negativeOutcomeCriterion = negativeOutcomeCriterion;
	}

	public CohortCriteria getBaseCriteria() {
		return baseCriteria;
	}

	public EventCriterion getTreatmentCriterion() {
		return treatmentCriterion;
	}

	public EventCriterion getNegativeOutcomeCriterion() {
		return negativeOutcomeCriterion;
	}
}
