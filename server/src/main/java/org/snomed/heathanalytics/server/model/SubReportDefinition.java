package org.snomed.heathanalytics.server.model;

import java.util.Objects;

public class SubReportDefinition {

	private String name;
	private CohortCriteria criteria;

	public SubReportDefinition() {
	}

	public SubReportDefinition(String name, CohortCriteria criteria) {
		this.name = name;
		this.criteria = criteria;
	}

	public String getName() {
		return name;
	}

	public SubReportDefinition setName(String name) {
		this.name = name;
		return this;
	}

	public CohortCriteria getCriteria() {
		return criteria;
	}

	public SubReportDefinition setCriteria(CohortCriteria criteria) {
		this.criteria = criteria;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SubReportDefinition that = (SubReportDefinition) o;
		return name.equals(that.name) &&
				criteria.equals(that.criteria);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, criteria);
	}
}
