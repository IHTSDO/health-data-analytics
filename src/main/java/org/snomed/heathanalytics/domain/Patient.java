package org.snomed.heathanalytics.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.Comparator;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

@Document(indexName = "patient")
public class Patient {

	@Id
	private String roleId;
	private String name;
	private Date dob;
	private Sex sex;

	private Set<ClinicalEncounter> encounters;

	public static final String FIELD_ID = "roleId";
	public static final String FIELD_NAME = "name";
	public static final String FIELD_DOB = "dob";
	public static final String FIELD_SEX = "sex";

	public Patient() {
	}

	public Patient(String roleId, String name, Date dob, Sex sex) {
		this.roleId = roleId;
		this.name = name;
		this.dob = dob;
		this.sex = sex;
	}

	public void addEncounter(ClinicalEncounter encounter) {
		if (encounters == null) {
			encounters = new TreeSet<>(Comparator.comparing(ClinicalEncounter::getDate));
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

	public void setDob(Date dob) {
		this.dob = dob;
	}

	public Sex getSex() {
		return sex;
	}

	public void setSex(Sex sex) {
		this.sex = sex;
	}

	@Override
	public String toString() {
		return "Patient{" +
				"roleId='" + roleId + '\'' +
				", name='" + name + '\'' +
				", sex=" + sex +
				", dob=" + dob +
				", encounters=" + encounters +
				'}';
	}
}
