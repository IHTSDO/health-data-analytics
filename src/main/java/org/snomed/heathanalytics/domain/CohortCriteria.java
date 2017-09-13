package org.snomed.heathanalytics.domain;

import org.snomed.heathanalytics.service.Criterion;
import org.snomed.heathanalytics.service.RelativeCriterion;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "cohort")
public class CohortCriteria {

	@Id
	private String id;

	private String name;

	private String description;

	@Field(type = FieldType.Object)
	private Criterion primaryExposure;

	@Field(type = FieldType.Object)
	private RelativeCriterion inclusionCriteria;

	public CohortCriteria() {
	}

	public CohortCriteria(Criterion primaryExposure) {
		this.primaryExposure = primaryExposure;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Criterion getPrimaryExposure() {
		return primaryExposure;
	}

	public void setPrimaryExposure(Criterion primaryExposure) {
		this.primaryExposure = primaryExposure;
	}

	public RelativeCriterion getInclusionCriteria() {
		return inclusionCriteria;
	}

	public void setInclusionCriteria(RelativeCriterion inclusionCriteria) {
		this.inclusionCriteria = inclusionCriteria;
	}
}
