package org.snomed.heathanalytics.server.model;

import java.util.ArrayList;
import java.util.List;

public class ReportDefinition extends SubReportDefinition {

	private List<List<SubReportDefinition>> groups;

	public ReportDefinition() {
		groups = new ArrayList<>();
	}

	public List<List<SubReportDefinition>> getGroups() {
		return groups;
	}

	public ReportDefinition setGroups(List<List<SubReportDefinition>> groups) {
		this.groups = groups;
		return this;
	}

	public ReportDefinition addReportToFirstListOfGroups(SubReportDefinition group) {
		if (groups.isEmpty()) {
			groups.add(new ArrayList<>());
		}
		groups.get(0).add(group);
		return this;
	}

}
