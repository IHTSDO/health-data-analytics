package org.snomed.heathanalytics.server.model;

import java.util.Map;

public class PatientPageWithCPTTotals extends PatientPageWithSearchAfter {

	private final Map<String, CPTTotals> cptTotals;

	public PatientPageWithCPTTotals(PatientPageWithSearchAfter page, Map<String, CPTTotals> cptTotals) {
		super(page.getContent(), page.getPageable(), page.getTotalElements(), page.getSearchAfter());
		this.cptTotals = cptTotals;
	}

	public Map<String, CPTTotals> getCptTotals() {
		return cptTotals;
	}
}
