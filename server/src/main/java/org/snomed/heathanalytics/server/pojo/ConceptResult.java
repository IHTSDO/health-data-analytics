package org.snomed.heathanalytics.server.pojo;

public class ConceptResult {

	private String code;
	private String display;

	public ConceptResult(String code, String display) {
		this.code = code;
		this.display = display;
	}

	public String getCode() {
		return code;
	}

	public String getDisplay() {
		return display;
	}
}
