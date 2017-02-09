package org.snomed.heathanalytics.domain;

import java.util.Date;

public class ClinicalEncounter implements Act {

	private Date date;
	private String conceptId;

	public ClinicalEncounter() {
	}

	public ClinicalEncounter(Date date, String conceptId) {
		this.date = date;
		this.conceptId = conceptId;
	}

	@Override
	public Date getDate() {
		return date;
	}

	public String getConceptId() {
		return conceptId;
	}
}
