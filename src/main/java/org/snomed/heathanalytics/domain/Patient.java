package org.snomed.heathanalytics.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.*;

@Document(indexName = "patient")
public class Patient {

	@Id
	private String roleId;

	@Field(index = FieldIndex.not_analyzed)
	private String name;

	@Field(index = FieldIndex.not_analyzed)
	private Date dob;

	@Field(index = FieldIndex.not_analyzed)
	private Gender gender;

	@Transient
	private Set<ClinicalEncounter> encounters;

	public interface Fields {
		String ROLE_ID = "roleId";
		String NAME = "name";
		String DOB = "dob";
		String SEX = "gender";
	}

	public Patient() {
	}

	public Patient(String roleId, String name, Date dob, Gender gender) {
		this.roleId = roleId;
		this.name = name;
		this.dob = dob;
		this.gender = gender;
	}

	public void addEncounter(ClinicalEncounter encounter) {
		if (encounters == null) {
			encounters = new HashSet<>();
		}
		encounters.add(encounter);
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getDob() {
		return dob;
	}

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	public Date getDobFormated() {
		return dob;
	}

	public void setDob(Date dob) {
		this.dob = dob;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	@Override
	public String toString() {
		return "Patient{" +
				"roleId='" + roleId + '\'' +
				", name='" + name + '\'' +
				", gender=" + gender +
				", dob=" + dob +
				", encounters=" + encounters +
				'}';
	}
}
