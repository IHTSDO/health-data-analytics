package org.snomed.heathanalytics.server.ingestion.fhir;

import org.elasticsearch.common.Strings;

import java.util.Date;

public class FHIRProcedure {

	private FHIRReference subject;
	private FHIRCodeableConcept code;
	private String status;
	private Date performedDateTime;
	private FHIRPeriod performedPeriod;

	public boolean isComplete() {
		return "completed".equals(status);
	}

	public Date getStartDate() {
		if (performedDateTime != null) {
			return performedDateTime;
		}
		if (performedPeriod != null) {
			return performedPeriod.getStart();
		}
		return null;
	}

	public FHIRReference getSubject() {
		return subject;
	}

	public FHIRCodeableConcept getCode() {
		return code;
	}

	public String getStatus() {
		return status;
	}

	public Date getPerformedDateTime() {
		return performedDateTime;
	}

	public FHIRPeriod getPerformedPeriod() {
		return performedPeriod;
	}
}
