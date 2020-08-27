package org.snomed.heathanalytics.server.ingestion.fhir;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class FHIRPatient {

	private String id;
	private String gender;
	private Date birthDate;

	public String getId() {
		return id;
	}

	public String getGender() {
		return gender;
	}

	@JsonFormat(pattern = "yyyy-MM-dd")
	public Date getBirthDate() {
		return birthDate;
	}

}
