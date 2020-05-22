package org.snomed.heathanalytics.server.model;

public class CPTCode {

	private String cptCode;
	private String workRVU;
	private String facilityPracticeExpenseRVU;
	private String nonfacilityPracticeExpenseRVU;
	private String pliRVU;
	private String totalFacilityRVU;
	private String totalMedicarePhysicianFeeScheduleFacilityPayment;
	private String totalNonfacilityRVU;
	private String totalMedicarePhysicianFeeScheduleNonFacilityPayment;

	@SuppressWarnings("unused")// For Jackson
	public CPTCode() {
	}

	public CPTCode(String cptCode, String workRVU, String facilityPracticeExpenseRVU, String nonfacilityPracticeExpenseRVU, String pliRVU,
			String totalFacilityRVU, String totalMedicarePhysicianFeeScheduleFacilityPayment, String totalNonfacilityRVU, String totalMedicarePhysicianFeeScheduleNonFacilityPayment) {
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

	public String getWorkRVU() {
		return workRVU;
	}

	public String getFacilityPracticeExpenseRVU() {
		return facilityPracticeExpenseRVU;
	}

	public String getNonfacilityPracticeExpenseRVU() {
		return nonfacilityPracticeExpenseRVU;
	}

	public String getPliRVU() {
		return pliRVU;
	}

	public String getTotalFacilityRVU() {
		return totalFacilityRVU;
	}

	public String getTotalMedicarePhysicianFeeScheduleFacilityPayment() {
		return totalMedicarePhysicianFeeScheduleFacilityPayment;
	}

	public String getTotalNonfacilityRVU() {
		return totalNonfacilityRVU;
	}

	public String getTotalMedicarePhysicianFeeScheduleNonFacilityPayment() {
		return totalMedicarePhysicianFeeScheduleNonFacilityPayment;
	}
}
