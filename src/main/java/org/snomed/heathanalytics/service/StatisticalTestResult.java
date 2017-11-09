package org.snomed.heathanalytics.service;

import java.math.BigDecimal;
import java.text.NumberFormat;

public class StatisticalTestResult {

	private int patientTotal;

	private int hasTestVariableCount;
	private int hasTestVariableHasOutcomeCount;

	private int hasNotTestVariableCount;
	private int hasNotTestVariableHasOutcomeCount;

	public StatisticalTestResult(int patientTotal, int hasTestVariableHasOutcomeCount, int hasTestVariableCount, int hasNotTestVariableHasOutcomeCount, int hasNotTestVariableCount) {
		this.patientTotal = patientTotal;
		this.hasTestVariableHasOutcomeCount = hasTestVariableHasOutcomeCount;
		this.hasTestVariableCount = hasTestVariableCount;
		this.hasNotTestVariableHasOutcomeCount = hasNotTestVariableHasOutcomeCount;
		this.hasNotTestVariableCount = hasNotTestVariableCount;
	}

	public int getPatientTotal() {
		return patientTotal;
	}

	public String getPatientTotalFormatted() {
		return NumberFormat.getNumberInstance().format(getPatientTotal());
	}

	public int getCohortTotal() {
		return hasTestVariableCount + hasNotTestVariableCount;
	}

	public String getCohortTotalFormatted() {
		return NumberFormat.getNumberInstance().format(getCohortTotal());
	}

	public int getHasTestVariableCount() {
		return hasTestVariableCount;
	}

	public String getHasTestVariableCountFormatted() {
		return NumberFormat.getNumberInstance().format(getHasTestVariableCount());
	}

	public String getHasTestVariablePercentage() {
		return getFractionAsPercentage(hasTestVariableCount, getCohortTotal());
	}

	public int getHasTestVariableHasOutcomeCount() {
		return hasTestVariableHasOutcomeCount;
	}

	public String getHasTestVariableHasOutcomeCountFormatted() {
		return NumberFormat.getNumberInstance().format(getHasTestVariableHasOutcomeCount());
	}

	public String getHasTestVariableChanceOfOutcome() {
		return getFractionAsPercentage(hasTestVariableHasOutcomeCount, hasTestVariableCount);
	}

	public int getHasNotTestVariableCount() {
		return hasNotTestVariableCount;
	}

	public String getHasNotTestVariableCountFormatted() {
		return NumberFormat.getNumberInstance().format(getHasNotTestVariableCount());
	}

	public int getHasNotTestVariableHasOutcomeCount() {
		return hasNotTestVariableHasOutcomeCount;
	}

	public String getHasNotTestVariableHasOutcomeCountFormatted() {
		return NumberFormat.getNumberInstance().format(getHasNotTestVariableHasOutcomeCount());
	}

	public String getHasNotTestVariableChanceOfOutcome() {
		return getFractionAsPercentage(hasNotTestVariableHasOutcomeCount, hasNotTestVariableCount);
	}

	public String getVariableOutcomeHazardRatio() {
		return new BigDecimal(((float)hasTestVariableHasOutcomeCount / (float)hasTestVariableCount)/
				((float)hasNotTestVariableHasOutcomeCount / (float)hasNotTestVariableCount)).setScale(2, BigDecimal.ROUND_HALF_UP).toString();
	}

	private String getFractionAsPercentage(int a, int b) {
		return new BigDecimal(((float) a / (float) b) * 100f).setScale(1, BigDecimal.ROUND_HALF_UP).toString();
	}

}
