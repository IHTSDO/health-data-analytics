package org.snomed.heathanalytics.domain;

import java.util.*;

public class Patient {

	private String name;
	private Date dob;
	private Sex sex;
	private Set<Act> acts;

	public Patient() {
		acts = new TreeSet<>(Act.ACT_DATE_COMPARATOR);
	}

	public Patient(String name, Date dob, Sex sex) {
		this();
		this.name = name;
		this.dob = dob;
		this.sex = sex;
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

	public Set<Act> getActs() {
		return acts;
	}

	public void setActs(Set<Act> acts) {
		this.acts = acts;
	}
}
