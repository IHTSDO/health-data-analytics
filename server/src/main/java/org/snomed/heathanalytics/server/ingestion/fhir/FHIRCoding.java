package org.snomed.heathanalytics.server.ingestion.fhir;

public class FHIRCoding {

	private String system;
	private String code;

	public FHIRCoding() {
	}

	public FHIRCoding(String system, String code) {
		this.system = system;
		this.code = code;
	}

	public String getSystem() {
		return system;
	}

	public String getCode() {
		return code;
	}
}
