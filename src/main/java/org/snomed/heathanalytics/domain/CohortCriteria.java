package org.snomed.heathanalytics.domain;

import java.util.ArrayList;
import java.util.List;

public class CohortCriteria {

	private Gender gender;
	private Integer minAgeNow;
	private Integer maxAgeNow;
	private final List<EncounterCriterion> encounterCriteria;

	public CohortCriteria() {
		encounterCriteria = new ArrayList<>();
	}

	public CohortCriteria(EncounterCriterion encounterCriterion) {
		this();
		addEncounterCriterion(encounterCriterion);
	}

	public void addEncounterCriterion(EncounterCriterion criterion) {
		encounterCriteria.add(criterion);
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public Integer getMinAgeNow() {
		return minAgeNow;
	}

	public void setMinAgeNow(Integer minAgeNow) {
		this.minAgeNow = minAgeNow;
	}

	public Integer getMaxAgeNow() {
		return maxAgeNow;
	}

	public void setMaxAgeNow(Integer maxAgeNow) {
		this.maxAgeNow = maxAgeNow;
	}

	public List<EncounterCriterion> getEncounterCriteria() {
		return encounterCriteria;
	}
}
