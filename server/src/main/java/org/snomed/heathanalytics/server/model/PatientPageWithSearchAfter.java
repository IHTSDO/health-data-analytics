package org.snomed.heathanalytics.server.model;

import org.snomed.heathanalytics.model.Patient;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class PatientPageWithSearchAfter extends PageImpl<Patient> {

	private final String searchAfter;

	public PatientPageWithSearchAfter(List<Patient> content, Pageable pageable, long total, String searchAfter) {
		super(content, pageable, total);
		this.searchAfter = searchAfter;
	}

	public String getSearchAfter() {
		return searchAfter;
	}
}
