package org.snomed.heathanalytics.server.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.snomed.heathanalytics.model.Gender;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonPropertyOrder({"gender", "minAgeNow", "maxAgeNow", "encounterCriteria", "exclusionCriteria"})
public class CohortCriteria {

	private Gender gender;
	private Integer minAgeNow;
	private Integer maxAgeNow;
	private final List<EncounterCriterion> encounterCriteria;
	private final List<CohortCriteria> exclusionCriteria;

	public CohortCriteria() {
		encounterCriteria = new ArrayList<>();
		exclusionCriteria = new ArrayList<>();
	}

	public CohortCriteria(Gender gender, Integer minAgeNow, Integer maxAgeNow) {
		this();
		this.gender = gender;
		this.minAgeNow = minAgeNow;
		this.maxAgeNow = maxAgeNow;
	}

	public CohortCriteria(EncounterCriterion encounterCriterion) {
		this();
		addEncounterCriterion(encounterCriterion);
	}

	public CohortCriteria addEncounterCriterion(EncounterCriterion criterion) {
		encounterCriteria.add(criterion);
		return this;
	}

	public CohortCriteria clone() {
		CohortCriteria cohortCriteria = new CohortCriteria();
		cohortCriteria.gender = gender;
		cohortCriteria.minAgeNow = minAgeNow;
		cohortCriteria.maxAgeNow = maxAgeNow;
		for (EncounterCriterion encounterCriterion : encounterCriteria) {
			cohortCriteria.addEncounterCriterion(encounterCriterion.clone());
		}
		return cohortCriteria;
	}

	/**
	 * Conditionally copies criterion from the supplied PatientCriteria to this one
	 * if the supplied values are more specific.
	 * @param criteriaToCopyFrom criteria to copy from.
	 */
	public void copyCriteriaWhereMoreSpecific(CohortCriteria criteriaToCopyFrom) {
		if (criteriaToCopyFrom == null) {
			return;
		}
		if (gender == null) {
			gender = criteriaToCopyFrom.gender;
		}
		if (criteriaToCopyFrom.minAgeNow != null &&
				(minAgeNow == null || minAgeNow < criteriaToCopyFrom.minAgeNow)) {
			minAgeNow = criteriaToCopyFrom.minAgeNow;
		}
		if (criteriaToCopyFrom.maxAgeNow != null &&
				(maxAgeNow == null || maxAgeNow > criteriaToCopyFrom.maxAgeNow)) {
			maxAgeNow = criteriaToCopyFrom.maxAgeNow;
		}
		encounterCriteria.addAll(criteriaToCopyFrom.encounterCriteria);
		exclusionCriteria.addAll(criteriaToCopyFrom.exclusionCriteria);
	}

	public Gender getGender() {
		return gender;
	}

	public CohortCriteria setGender(Gender gender) {
		this.gender = gender;
		return this;
	}

	public Integer getMinAgeNow() {
		return minAgeNow;
	}

	public CohortCriteria setMinAgeNow(Integer minAgeNow) {
		this.minAgeNow = minAgeNow;
		return this;
	}

	public Integer getMaxAgeNow() {
		return maxAgeNow;
	}

	public CohortCriteria setMaxAgeNow(Integer maxAgeNow) {
		this.maxAgeNow = maxAgeNow;
		return this;
	}

	public List<EncounterCriterion> getEncounterCriteria() {
		return encounterCriteria;
	}

	public List<CohortCriteria> getExclusionCriteria() {
		return exclusionCriteria;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CohortCriteria that = (CohortCriteria) o;
		return gender == that.gender &&
				Objects.equals(minAgeNow, that.minAgeNow) &&
				Objects.equals(maxAgeNow, that.maxAgeNow) &&
				Objects.equals(encounterCriteria, that.encounterCriteria) &&
				Objects.equals(exclusionCriteria, that.exclusionCriteria);
	}

	@Override
	public int hashCode() {
		return Objects.hash(gender, minAgeNow, maxAgeNow, encounterCriteria, exclusionCriteria);
	}
}
