package org.snomed.heathanalytics.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.Date;

@Document(indexName = "patient")
public class Patient {

	@Id
	public String roleId;
	private String name;
	private Date dob;
	private Sex sex;

	public Patient() {
	}

	public Patient(String roleId, String name, Date dob, Sex sex) {
		this.roleId = roleId;
		this.name = name;
		this.dob = dob;
		this.sex = sex;
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
				'}';
	}
}
