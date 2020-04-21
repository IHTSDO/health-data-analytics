package org.snomed.heathanalytics.server.model;

public class StatisticalCorrelationReportDefinition {

	private CohortCriteria baseCriteria;
	private EncounterCriterion treatmentCriterion;
	private EncounterCriterion negativeOutcomeCriterion;

	public StatisticalCorrelationReportDefinition() {
	}

	public StatisticalCorrelationReportDefinition(CohortCriteria baseCriteria, EncounterCriterion treatmentCriterion, EncounterCriterion negativeOutcomeCriterion) {
		this.baseCriteria = baseCriteria;
		this.treatmentCriterion = treatmentCriterion;
		this.negativeOutcomeCriterion = negativeOutcomeCriterion;
	}

	public CohortCriteria getBaseCriteria() {
		return baseCriteria;
	}

	public EncounterCriterion getTreatmentCriterion() {
		return treatmentCriterion;
	}

	public EncounterCriterion getNegativeOutcomeCriterion() {
		return negativeOutcomeCriterion;
	}
}
