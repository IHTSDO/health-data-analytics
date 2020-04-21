package org.snomed.heathanalytics.server.model;

import java.util.List;

public class ReportDefinition extends SubReportDefinition {

	private List<List<SubReportDefinition>> groups;

	public ReportDefinition() {
	}

	public List<List<SubReportDefinition>> getGroups() {
		return groups;
	}

	public void setGroups(List<List<SubReportDefinition>> groups) {
		this.groups = groups;
	}
}
