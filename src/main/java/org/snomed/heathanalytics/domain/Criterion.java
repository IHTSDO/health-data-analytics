package org.snomed.heathanalytics.domain;

public class Criterion {

	private String subsetId;
	private String ecl;

	public Criterion() {
	}

	public Criterion(String ecl) {
		this.ecl = ecl;
	}

	public String getSubsetId() {
		return subsetId;
	}

	public void setSubsetId(String subsetId) {
		this.subsetId = subsetId;
	}

	public String getEcl() {
		return ecl;
	}

	public void setEcl(String ecl) {
		this.ecl = ecl;
	}

}
