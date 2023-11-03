package org.snomed.heathanalytics.server.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Date;
import java.util.Objects;

@JsonPropertyOrder({"has", "conceptECL", "conceptSubsetId", "frequency",
		"withinDaysBeforePreviouslyMatchedEvent", "withinDaysAfterPreviouslyMatchedEvent"})
public class EventCriterion {

	private boolean has;
	private String conceptECL;
	private String conceptSubsetId;
	private Date minDate;
	private Date maxDate;

	private Frequency frequency;

	// Value of -1 means apply constraint with unbounded value, otherwise use null.
	private Integer withinDaysBeforePreviouslyMatchedEvent;

	// Value of -1 means apply constraint with unbounded value, otherwise use null
	private Integer withinDaysAfterPreviouslyMatchedEvent;

	private boolean includeCPTAnalysis = false;

	public EventCriterion() {
		has = true;
	}

	public EventCriterion(String conceptECL) {
		this();
		this.conceptECL = conceptECL;
	}

	public EventCriterion(String conceptECL, Integer withinDaysAfterPreviouslyMatchedEvent, Integer withinDaysBeforePreviouslyMatchedEvent) {
		this();
		this.conceptECL = conceptECL;
		this.withinDaysAfterPreviouslyMatchedEvent = withinDaysAfterPreviouslyMatchedEvent;
		this.withinDaysBeforePreviouslyMatchedEvent = withinDaysBeforePreviouslyMatchedEvent;
	}

	public EventCriterion clone() {
		EventCriterion eventCriterion = new EventCriterion();
		eventCriterion.has = has;
		eventCriterion.conceptECL = conceptECL;
		eventCriterion.conceptSubsetId = conceptSubsetId;
		eventCriterion.minDate = minDate;
		eventCriterion.maxDate = maxDate;
		eventCriterion.frequency = frequency;
		return eventCriterion;
	}

	public boolean hasTimeConstraint() {
		return withinDaysAfterPreviouslyMatchedEvent != null ||
				withinDaysBeforePreviouslyMatchedEvent != null ||
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

	public Integer getWithinDaysBeforePreviouslyMatchedEvent() {
		return withinDaysBeforePreviouslyMatchedEvent;
	}

	public void setWithinDaysBeforePreviouslyMatchedEvent(Integer withinDaysBeforePreviouslyMatchedEvent) {
		this.withinDaysBeforePreviouslyMatchedEvent = withinDaysBeforePreviouslyMatchedEvent;
	}

	public Integer getWithinDaysAfterPreviouslyMatchedEvent() {
		return withinDaysAfterPreviouslyMatchedEvent;
	}

	public void setWithinDaysAfterPreviouslyMatchedEvent(Integer withinDaysAfterPreviouslyMatchedEvent) {
		this.withinDaysAfterPreviouslyMatchedEvent = withinDaysAfterPreviouslyMatchedEvent;
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

	public EventCriterion setFrequency(Frequency frequency) {
		this.frequency = frequency;
		return this;
	}

	public boolean isIncludeCPTAnalysis() {
		return includeCPTAnalysis;
	}

	public EventCriterion setIncludeCPTAnalysis(boolean includeCPTAnalysis) {
		this.includeCPTAnalysis = includeCPTAnalysis;
		return this;
	}

	public EventCriterion includeCPTAnalysis() {
		this.includeCPTAnalysis = true;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EventCriterion that = (EventCriterion) o;
		return has == that.has &&
				Objects.equals(conceptECL, that.conceptECL) &&
				Objects.equals(conceptSubsetId, that.conceptSubsetId) &&
				Objects.equals(withinDaysBeforePreviouslyMatchedEvent, that.withinDaysBeforePreviouslyMatchedEvent) &&
				Objects.equals(withinDaysAfterPreviouslyMatchedEvent, that.withinDaysAfterPreviouslyMatchedEvent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(has, conceptECL, conceptSubsetId, withinDaysBeforePreviouslyMatchedEvent, withinDaysAfterPreviouslyMatchedEvent);
	}
}
