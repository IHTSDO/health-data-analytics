package org.snomed.heathanalytics.server.ingestion.fhir;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class FHIRPatient {

	private String id;
	private String gender;
	private Date birthDate;

	public FHIRPatient() {
	}

	public FHIRPatient(String id, String gender, Date birthDate) {
		this.id = id;
		this.gender = gender;
		this.birthDate = birthDate;
	}

	public String getResourceType() {
		return "Patient";
	}

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
