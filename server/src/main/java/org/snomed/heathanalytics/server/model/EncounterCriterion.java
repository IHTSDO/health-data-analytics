package org.snomed.heathanalytics.server.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Date;
import java.util.Objects;

@JsonPropertyOrder({"has", "conceptECL", "conceptSubsetId", "frequency",
		"withinDaysBeforePreviouslyMatchedEncounter", "withinDaysAfterPreviouslyMatchedEncounter"})
public class EncounterCriterion {

	private boolean has;
	private String conceptECL;
	private String conceptSubsetId;
	private Date minDate;
	private Date maxDate;

	private Frequency frequency;

	// Value of -1 means apply constraint with unbounded value, otherwise use null.
	private Integer withinDaysBeforePreviouslyMatchedEncounter;

	// Value of -1 means apply constraint with unbounded value, otherwise use null
	private Integer withinDaysAfterPreviouslyMatchedEncounter;

	private boolean includeCPTAnalysis = false;

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

	public EncounterCriterion clone() {
		EncounterCriterion encounterCriterion = new EncounterCriterion();
		encounterCriterion.has = has;
		encounterCriterion.conceptECL = conceptECL;
		encounterCriterion.conceptSubsetId = conceptSubsetId;
		encounterCriterion.minDate = minDate;
		encounterCriterion.maxDate = maxDate;
		encounterCriterion.frequency = frequency;
		return encounterCriterion;
	}

	public boolean hasTimeConstraint() {
		return withinDaysAfterPreviouslyMatchedEncounter != null ||
				withinDaysBeforePreviouslyMatchedEncounter != null ||
				minDate != null ||
				maxDate != null;
	}

	public boolean hasFrequency() {
		return frequency != null;
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

	public Date getMinDate() {
		return minDate;
	}

	public void setMinDate(Date minDate) {
		this.minDate = minDate;
	}

	public Date getMaxDate() {
		return maxDate;
	}

	public void setMaxDate(Date maxDate) {
		this.maxDate = maxDate;
	}

	public Frequency getFrequency() {
		return frequency;
	}

	public EncounterCriterion setFrequency(Frequency frequency) {
		this.frequency = frequency;
		return this;
	}

	public boolean isIncludeCPTAnalysis() {
		return includeCPTAnalysis;
	}

	public EncounterCriterion setIncludeCPTAnalysis(boolean includeCPTAnalysis) {
		this.includeCPTAnalysis = includeCPTAnalysis;
		return this;
	}

	public EncounterCriterion includeCPTAnalysis() {
		this.includeCPTAnalysis = true;
		return this;
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
