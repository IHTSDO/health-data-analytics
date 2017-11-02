package org.snomed.heathanalytics.service;

import java.math.BigDecimal;

public class StatisticalTestResult {

	private int hasTestVariableHasOutcomeCount;
	private int hasTestVariableCount;

	private int hasNotTestVariableHasOutcomeCount;
	private int hasNotTestVariableCount;

	public StatisticalTestResult(int hasTestVariableHasOutcomeCount, int hasTestVariableCount, int hasNotTestVariableHasOutcomeCount, int hasNotTestVariableCount) {
		this.hasTestVariableHasOutcomeCount = hasTestVariableHasOutcomeCount;
		this.hasTestVariableCount = hasTestVariableCount;
		this.hasNotTestVariableHasOutcomeCount = hasNotTestVariableHasOutcomeCount;
		this.hasNotTestVariableCount = hasNotTestVariableCount;
	}

	public int getHasTestVariableHasOutcomeCount() {
		return hasTestVariableHasOutcomeCount;
	}

	public int getHasTestVariableCount() {
		return hasTestVariableCount;
	}

	public float getHasTestVariableChanceOfOutcome() {
		return new BigDecimal((float) hasTestVariableHasOutcomeCount / (float) hasTestVariableCount).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
	}

	public int getHasNotTestVariableHasOutcomeCount() {
		return hasNotTestVariableHasOutcomeCount;
	}

	public int getHasNotTestVariableCount() {
		return hasNotTestVariableCount;
	}

	public float getHasNotTestVariableChanceOfOutcome() {
		return new BigDecimal((float) hasNotTestVariableHasOutcomeCount / (float) hasNotTestVariableCount).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();

	}

}
