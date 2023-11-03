package org.snomed.heathanalytics.server.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;

public class StatisticalCorrelationReport {

	private final int allPatientsCount;

	private final int withTreatmentCount;
	private final int withTreatmentWithNegativeOutcomeCount;

	private final int withoutTreatmentCount;
	private final int withoutTreatmentWithNegativeOutcomeCount;

	public StatisticalCorrelationReport(int allPatientsCount, int withTreatmentCount, int withTreatmentWithNegativeOutcomeCount, int withoutTreatmentCount, int withoutTreatmentWithNegativeOutcomeCount) {
		this.allPatientsCount = allPatientsCount;
		this.withTreatmentCount = withTreatmentCount;
		this.withTreatmentWithNegativeOutcomeCount = withTreatmentWithNegativeOutcomeCount;
		this.withoutTreatmentCount = withoutTreatmentCount;
		this.withoutTreatmentWithNegativeOutcomeCount = withoutTreatmentWithNegativeOutcomeCount;
	}

	public int getAllPatientsCount() {
		return allPatientsCount;
	}

	public String getAllPatientsCountFormatted() {
		return NumberFormat.getNumberInstance().format(getAllPatientsCount());
	}

	public int getCohortTotal() {
		return withTreatmentCount + withoutTreatmentCount;
	}

	public String getCohortTotalFormatted() {
		return NumberFormat.getNumberInstance().format(getCohortTotal());
	}

	public int getWithTreatmentCount() {
		return withTreatmentCount;
	}

	public String getWithTreatmentCountFormatted() {
		return NumberFormat.getNumberInstance().format(getWithTreatmentCount());
	}

	public String getWithTreatmentPercentage() {
		return getFractionAsPercentage(withTreatmentCount, getCohortTotal());
	}

	public String getWithoutTreatmentPercentage() {
		return getFractionAsPercentage(withoutTreatmentCount, getCohortTotal());
	}

	public int getWithTreatmentWithNegativeOutcomeCount() {
		return withTreatmentWithNegativeOutcomeCount;
	}

	public String getWithTreatmentWithNegativeOutcomeCountFormatted() {
		return NumberFormat.getNumberInstance().format(getWithTreatmentWithNegativeOutcomeCount());
	}

	public String getWithTreatmentChanceOfNegativeOutcome() {
		return getFractionAsPercentage(withTreatmentWithNegativeOutcomeCount, withTreatmentCount);
	}

	public int getWithoutTreatmentCount() {
		return withoutTreatmentCount;
	}

	public String getWithoutTreatmentCountFormatted() {
		return NumberFormat.getNumberInstance().format(getWithoutTreatmentCount());
	}

	public int getWithoutTreatmentWithNegativeOutcomeCount() {
		return withoutTreatmentWithNegativeOutcomeCount;
	}

	public String getWithoutTreatmentWithNegativeOutcomeCountFormatted() {
		return NumberFormat.getNumberInstance().format(getWithoutTreatmentWithNegativeOutcomeCount());
	}

	public String getWithoutTreatmentChanceOfNegativeOutcome() {
		return getFractionAsPercentage(withoutTreatmentWithNegativeOutcomeCount, withoutTreatmentCount);
	}

	public String getTreatmentNegativeOutcomeHazardRatio() {
		if (withTreatmentWithNegativeOutcomeCount == 0 || withTreatmentCount == 0 ||
				withoutTreatmentWithNegativeOutcomeCount == 0 || withoutTreatmentCount == 0) return "-";

		return BigDecimal.valueOf(((float) withTreatmentWithNegativeOutcomeCount / (float) withTreatmentCount) /
				((float) withoutTreatmentWithNegativeOutcomeCount / (float) withoutTreatmentCount)).setScale(2,  RoundingMode.HALF_UP).toString();
	}

	private String getFractionAsPercentage(int a, int b) {
		if (a == 0 || b == 0) return "-";
		return BigDecimal.valueOf(((float) a / (float) b) * 100f).setScale(1,  RoundingMode.HALF_UP).toString();
	}

	@Override
	public String toString() {
		return "StatisticalCorrelationReport{" +
				"allPatientsCount=" + allPatientsCount +
				", withTreatmentCount=" + withTreatmentCount +
				", withTreatmentWithNegativeOutcomeCount=" + withTreatmentWithNegativeOutcomeCount +
				", withoutTreatmentCount=" + withoutTreatmentCount +
				", withoutTreatmentWithNegativeOutcomeCount=" + withoutTreatmentWithNegativeOutcomeCount +
				'}';
	}
}
