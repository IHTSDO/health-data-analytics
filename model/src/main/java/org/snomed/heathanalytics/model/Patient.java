package org.snomed.heathanalytics.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.text.SimpleDateFormat;
import java.util.*;

@Document(indexName = "patient")
@JsonPropertyOrder({"roleId", "gender", "dobYear", "dob", "dobFormatted", "encounters"})
public class Patient {

	@Id
	private String roleId;

	@Field(type = FieldType.Long)
	private long dobLong;

	@Field(type = FieldType.Integer)
	private int dobYear;

	@Field(type = FieldType.Keyword)
	private Gender gender;

	private Set<ClinicalEncounter> encounters;

	public interface Fields {
		String ROLE_ID = "roleId";
		String DOB_LONG = "dobLong";
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

	@JsonView({View.API.class, View.Elasticsearch.class})
	public Set<ClinicalEncounter> getEncounters() {
		return encounters;
	}

	@JsonView({View.API.class, View.Elasticsearch.class})
	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	@JsonView(View.API.class)
	public Date getDob() {
		return new Date(dobLong);
	}

	@JsonView(View.Elasticsearch.class)
	public long getDobLong() {
		return dobLong;
	}

	public void setDobLong(long dobLong) {
		this.dobLong = dobLong;
	}

	public void setDob(Date dob) {
		this.dobLong = dob.getTime();
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(dob);
		this.dobYear = calendar.get(Calendar.YEAR);
	}

	@JsonView(View.Elasticsearch.class)
	public int getDobYear() {
		return dobYear;
	}

	public void setDobYear(int dobYear) {
		this.dobYear = dobYear;
	}

	@JsonView({View.API.class, View.Elasticsearch.class})
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
				", dob=" + new SimpleDateFormat("yyyy-MM-dd").format(new Date(dobLong)) +
				", encounters=" + encounters +
				'}';
	}
}
