package org.snomed.heathanalytics.server.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

@JsonPropertyOrder({"has", "conceptECL", "conceptSubsetId",
		"withinDaysBeforePreviouslyMatchedEncounter", "withinDaysAfterPreviouslyMatchedEncounter"})
public class EncounterCriterion {

	private boolean has;
	private String conceptECL;
	private String conceptSubsetId;

	// Value of -1 means apply constrain with unbounded value, otherwise use null.
	private Integer withinDaysBeforePreviouslyMatchedEncounter;

	// Value of -1 means apply constrain with unbounded value, otherwise use null
	private Integer withinDaysAfterPreviouslyMatchedEncounter;

	public EncounterCriterion() {
		has = true;
	}

	public EncounterCriterion(String conceptECL) {
		this();
		this.conceptECL = conceptECL;
	}

	public EncounterCriterion(String conceptECL, Integer withinDaysAfterPreviouslyMatchedEncounter, Integer withinDaysBeforePreviouslyMatchedEncounter) {
		this();
		this.conceptECL = conceptECL;
		this.withinDaysAfterPreviouslyMatchedEncounter = withinDaysAfterPreviouslyMatchedEncounter;
		this.withinDaysBeforePreviouslyMatchedEncounter = withinDaysBeforePreviouslyMatchedEncounter;
	}

	public boolean hasTimeConstraint() {
		return withinDaysAfterPreviouslyMatchedEncounter != null || withinDaysBeforePreviouslyMatchedEncounter != null;
	}

	public boolean isHas() {
		return has;
	}

	public void setHas(boolean has) {
		this.has = has;
	}

	public String getConceptECL() {
		return conceptECL;
	}

	public void setConceptECL(String conceptECL) {
		this.conceptECL = conceptECL;
	}

	public String getConceptSubsetId() {
		return conceptSubsetId;
	}

	public void setConceptSubsetId(String conceptSubsetId) {
		this.conceptSubsetId = conceptSubsetId;
	}

	public Integer getWithinDaysBeforePreviouslyMatchedEncounter() {
		return withinDaysBeforePreviouslyMatchedEncounter;
	}

	public void setWithinDaysBeforePreviouslyMatchedEncounter(Integer withinDaysBeforePreviouslyMatchedEncounter) {
		this.withinDaysBeforePreviouslyMatchedEncounter = withinDaysBeforePreviouslyMatchedEncounter;
	}

	public Integer getWithinDaysAfterPreviouslyMatchedEncounter() {
		return withinDaysAfterPreviouslyMatchedEncounter;
	}

	public void setWithinDaysAfterPreviouslyMatchedEncounter(Integer withinDaysAfterPreviouslyMatchedEncounter) {
		this.withinDaysAfterPreviouslyMatchedEncounter = withinDaysAfterPreviouslyMatchedEncounter;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EncounterCriterion that = (EncounterCriterion) o;
		return has == that.has &&
				Objects.equals(conceptECL, that.conceptECL) &&
				Objects.equals(conceptSubsetId, that.conceptSubsetId) &&
				Objects.equals(withinDaysBeforePreviouslyMatchedEncounter, that.withinDaysBeforePreviouslyMatchedEncounter) &&
				Objects.equals(withinDaysAfterPreviouslyMatchedEncounter, that.withinDaysAfterPreviouslyMatchedEncounter);
	}

	@Override
	public int hashCode() {
		return Objects.hash(has, conceptECL, conceptSubsetId, withinDaysBeforePreviouslyMatchedEncounter, withinDaysAfterPreviouslyMatchedEncounter);
	}
}
