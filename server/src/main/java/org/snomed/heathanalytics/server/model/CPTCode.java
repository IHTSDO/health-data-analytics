package org.snomed.heathanalytics.server.model;

public class CPTCode {

	private String cptCode;
	private Float workRVU;
	private Float facilityPracticeExpenseRVU;
	private Float nonfacilityPracticeExpenseRVU;
	private Float pliRVU;
	private Float totalFacilityRVU;
	private Float totalMedicarePhysicianFeeScheduleFacilityPayment;
	private Float totalNonfacilityRVU;
	private Float totalMedicarePhysicianFeeScheduleNonFacilityPayment;

	@SuppressWarnings("unused")// For Jackson
	public CPTCode() {
	}

	public CPTCode(String cptCode, Float workRVU, Float facilityPracticeExpenseRVU, Float nonfacilityPracticeExpenseRVU, Float pliRVU, Float totalFacilityRVU,
			Float totalMedicarePhysicianFeeScheduleFacilityPayment, Float totalNonfacilityRVU, Float totalMedicarePhysicianFeeScheduleNonFacilityPayment) {

		this.cptCode = cptCode;
		this.workRVU = workRVU;
		this.facilityPracticeExpenseRVU = facilityPracticeExpenseRVU;
		this.nonfacilityPracticeExpenseRVU = nonfacilityPracticeExpenseRVU;
		this.pliRVU = pliRVU;
		this.totalFacilityRVU = totalFacilityRVU;
		this.totalMedicarePhysicianFeeScheduleFacilityPayment = totalMedicarePhysicianFeeScheduleFacilityPayment;
		this.totalNonfacilityRVU = totalNonfacilityRVU;
		this.totalMedicarePhysicianFeeScheduleNonFacilityPayment = totalMedicarePhysicianFeeScheduleNonFacilityPayment;
	}

	public String getCptCode() {
		return cptCode;
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
		return "CPTCode{" +
				"cptCode='" + cptCode + '\'' +
				", workRVU=" + workRVU +
				'}';
	}
}
