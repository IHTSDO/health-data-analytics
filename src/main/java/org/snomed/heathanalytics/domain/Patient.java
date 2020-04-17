package org.snomed.heathanalytics.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.*;

@Document(indexName = "patient")
@JsonPropertyOrder({"roleId", "gender", "dobYear", "dob", "dobFormatted", "encounters"})
public class Patient {

	@Id
	private String roleId;

	@Field(type = FieldType.Date, index = FieldIndex.not_analyzed)
	private Date dob;

	@Field(type = FieldType.Integer)
	private int dobYear;

	@Field(type = FieldType.String, index = FieldIndex.not_analyzed)
	private Gender gender;

	private Set<ClinicalEncounter> encounters;

	public interface Fields {
		String ROLE_ID = "roleId";
		String DOB = "dob";
		String DOB_YEAR = "dobYear";
		String GENDER = "gender";
		String encounters = "encounters";
	}

	public Patient() {
	}

	public Patient(String roleId) {
		this.roleId = roleId;
	}

	public Patient(String roleId, Date dob, Gender gender) {
		this.roleId = roleId;
		setDob(dob);
		this.gender = gender;
	}

	public Patient addEncounter(ClinicalEncounter encounter) {
		if (encounters == null) {
			encounters = new HashSet<>();
		}
		encounters.add(encounter);
		return this;
	}

	public Set<ClinicalEncounter> getEncounters() {
		return encounters;
	}

	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	public Date getDob() {
		return dob;
	}

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	public Date getDobFormatted() {
		return dob;
	}

	public void setDob(Date dob) {
		this.dob = dob;
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(dob);
		this.dobYear = calendar.get(Calendar.YEAR);
	}

	public int getDobYear() {
		return dobYear;
	}

	public void setDobYear(int dobYear) {
		this.dobYear = dobYear;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public void setEncounters(Set<ClinicalEncounter> encounters) {
		this.encounters = encounters;
	}

	@Override
	public String toString() {
		return "Patient{" +
				"roleId='" + roleId + '\'' +
				", gender=" + gender +
				", dob=" + dob +
				", encounters=" + encounters +
				'}';
	}
}
