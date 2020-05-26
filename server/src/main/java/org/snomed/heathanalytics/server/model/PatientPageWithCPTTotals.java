package org.snomed.heathanalytics.server.model;

import org.snomed.heathanalytics.model.Patient;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public class PatientPageWithCPTTotals extends PageImpl<Patient> {

	private final Map<String, CPTTotals> cptTotals;

	public PatientPageWithCPTTotals(List<Patient> content, Pageable pageable, long total, Map<String, CPTTotals> cptTotals) {
		super(content, pageable, total);
		this.cptTotals = cptTotals;
	}

	public Map<String, CPTTotals> getCptTotals() {
		return cptTotals;
	}
}
