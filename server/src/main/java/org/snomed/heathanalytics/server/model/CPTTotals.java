package org.snomed.heathanalytics.server.model;

public class CPTTotals {

	private CPTCode cptCode;
	private int count;
	private Float workRVU;
	private Float facilityPracticeExpenseRVU;
	private Float nonfacilityPracticeExpenseRVU;
	private Float pliRVU;
	private Float totalFacilityRVU;
	private Float totalMedicarePhysicianFeeScheduleFacilityPayment;
	private Float totalNonfacilityRVU;
	private Float totalMedicarePhysicianFeeScheduleNonFacilityPayment;

	public CPTTotals(CPTCode cptCode) {
		this.cptCode = cptCode;
		calculateTotals();
	}

	public void addCount(int count) {
		this.count += count;
		calculateTotals();
	}

	private void calculateTotals() {
		workRVU = getTotal(cptCode.getWorkRVU(), count);
		facilityPracticeExpenseRVU = getTotal(cptCode.getFacilityPracticeExpenseRVU(), count);
		nonfacilityPracticeExpenseRVU = getTotal(cptCode.getNonfacilityPracticeExpenseRVU(), count);
		pliRVU = getTotal(cptCode.getPliRVU(), count);
		totalFacilityRVU = getTotal(cptCode.getTotalFacilityRVU(), count);
		totalMedicarePhysicianFeeScheduleFacilityPayment = getTotal(cptCode.getTotalMedicarePhysicianFeeScheduleFacilityPayment(), count);
		totalNonfacilityRVU = getTotal(cptCode.getTotalNonfacilityRVU(), count);
		totalMedicarePhysicianFeeScheduleNonFacilityPayment = getTotal(cptCode.getTotalMedicarePhysicianFeeScheduleNonFacilityPayment(), count);
	}

	private Float getTotal(Float value, int count) {
		return value != null ? value * count : null;
	}

	public CPTCode getCptCode() {
		return cptCode;
	}

	public int getCount() {
		return count;
	}

	public Float getWorkRVU() {
		return workRVU;
	}

	public Float getFacilityPracticeExpenseRVU() {
		return facilityPracticeExpenseRVU;
	}

	public Float getNonfacilityPracticeExpenseRVU() {
		return nonfacilityPracticeExpenseRVU;
	}

	public Float getPliRVU() {
		return pliRVU;
	}

	public Float getTotalFacilityRVU() {
		return totalFacilityRVU;
	}

	public Float getTotalMedicarePhysicianFeeScheduleFacilityPayment() {
		return totalMedicarePhysicianFeeScheduleFacilityPayment;
	}

	public Float getTotalNonfacilityRVU() {
		return totalNonfacilityRVU;
	}

	public Float getTotalMedicarePhysicianFeeScheduleNonFacilityPayment() {
		return totalMedicarePhysicianFeeScheduleNonFacilityPayment;
	}

	@Override
	public String toString() {
		return "CPTTotals{" +
				"cptCode=" + cptCode +
				", count=" + count +
				", workRVU=" + workRVU +
				'}';
	}
}
