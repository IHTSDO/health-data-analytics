package org.snomed.heathanalytics.domain;

import org.elasticsearch.common.Strings;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.ArrayList;
import java.util.List;

public class CohortCriteria {

	private Gender gender;
	private Integer minAge;
	private Integer maxAge;
	private EncounterCriterion primaryCriterion;
	private List<RelativeCriterion> additionalCriteria;
	private RelativeCriterion testVariable;
	private RelativeCriterion testOutcome;

	public CohortCriteria() {
		additionalCriteria = new ArrayList<>();
	}

	public CohortCriteria(EncounterCriterion primaryCriterion) {
		this();
		this.primaryCriterion = primaryCriterion;
	}

	public void addAdditionalCriterion(RelativeCriterion criterion) {
		additionalCriteria.add(criterion);
	}

	public boolean isEmptyPrimaryCriterion() {
		return primaryCriterion == null || (Strings.isNullOrEmpty(primaryCriterion.getEcl()) && Strings.isNullOrEmpty(primaryCriterion.getSubsetId()));
	}

	public boolean isRelativeEncounterCheckNeeded() {
		return !isEmptyPrimaryCriterion() && testVariable != null || additionalCriteria.stream().anyMatch(RelativeCriterion::hasTimeConstraint);
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public Integer getMinAge() {
		return minAge;
	}

	public void setMinAge(Integer minAge) {
		this.minAge = minAge;
	}

	public Integer getMaxAge() {
		return maxAge;
	}

	public void setMaxAge(Integer maxAge) {
		this.maxAge = maxAge;
	}

	public EncounterCriterion getPrimaryCriterion() {
		return primaryCriterion;
	}

	public void setPrimaryCriterion(EncounterCriterion primaryCriterion) {
		this.primaryCriterion = primaryCriterion;
	}

	public List<RelativeCriterion> getAdditionalCriteria() {
		return additionalCriteria;
	}

	public void setAdditionalCriteria(List<RelativeCriterion> additionalCriteria) {
		this.additionalCriteria = additionalCriteria;
	}

	public RelativeCriterion getTestVariable() {
		return testVariable;
	}

	public void setTestVariable(RelativeCriterion testVariable) {
		this.testVariable = testVariable;
	}

	public RelativeCriterion getTestOutcome() {
		return testOutcome;
	}

	public void setTestOutcome(RelativeCriterion testOutcome) {
		this.testOutcome = testOutcome;
	}
}
