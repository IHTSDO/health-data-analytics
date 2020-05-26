package org.snomed.heathanalytics.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Report {

	private final String name;
	private final int patientCount;
	private final CohortCriteria criteria;
	private Map<String, CPTTotals> cptTotals;
	private List<Report> groups;

	public Report(String name, int patientCount, CohortCriteria criteria) {
		this.name = name;
		this.patientCount = patientCount;
		this.criteria = criteria;
	}

	public Report(String name, int patientCount, CohortCriteria criteria, Map<String, CPTTotals> cptTotals) {
		this(name, patientCount, criteria);
		this.cptTotals = cptTotals;
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

	public Map<String, CPTTotals> getCptTotals() {
		return cptTotals;
	}

	public List<Report> getGroups() {
		return groups;
	}

	@Override
	public String toString() {
		return "Report{" +
				"name='" + name + '\'' +
				", patientCount=" + patientCount +
				", criteria=" + criteria +
				", cptTotals=" + cptTotals +
				", groups=" + groups +
				'}';
	}
}
