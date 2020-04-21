package org.snomed.heathanalytics.server.model;

import java.util.ArrayList;
import java.util.List;

public class Report {

	private final String name;
	private final int patientCount;
	private final CohortCriteria criteria;
	private List<Report> groups;

	public Report(String name, int patientCount, CohortCriteria criteria) {
		this.name = name;
		this.patientCount = patientCount;
		this.criteria = criteria;
	}

	public void addGroup(Report group) {
		if (groups == null) {
			groups = new ArrayList<>();
		}
		groups.add(group);
	}

	public String getName() {
		return name;
	}

	public int getPatientCount() {
		return patientCount;
	}

	public CohortCriteria getCriteria() {
		return criteria;
	}

	public List<Report> getGroups() {
		return groups;
	}
}
