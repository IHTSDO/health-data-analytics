package org.snomed.heathanalytics.domain;

import java.util.Objects;

public class SubReportDefinition {

	private String name;
	private CohortCriteria criteria;

	public SubReportDefinition() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public CohortCriteria getCriteria() {
		return criteria;
	}

	public void setCriteria(CohortCriteria criteria) {
		this.criteria = criteria;
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
