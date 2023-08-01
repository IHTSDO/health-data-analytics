package org.snomed.heathanalytics.server.ingestion.fhir;

import org.apache.commons.lang3.time.DateUtils;

import java.util.Date;

public class FHIRServiceRequest {

	public static final int ASSUME_COMPLETE_AFTER_DAYS = 14;

	private String intent;
	private Boolean doNotPerform;
	private String status;
	private Date authoredOn;
	private Date occurrenceDateTime;
	private FHIRReference subject;
	private FHIRCodeableConcept code;

	public boolean isCompleteOrLikelyComplete() {
		if (Boolean.TRUE.equals(doNotPerform) || "proposal".equals(intent)) {
			return false;
		}
		if ("completed".equals(status)) {
			return true;
		} else if ("active".equals(status) && getOccurrenceDateOrBestGuess().before(new Date())) {
			return true;
		}
		return false;
	}

	public Date getOccurrenceDateOrBestGuess() {
		return occurrenceDateTime != null ? occurrenceDateTime : DateUtils.addDays(authoredOn, ASSUME_COMPLETE_AFTER_DAYS);
	}

	public String getIntent() {
		return intent;
	}

	public Boolean getDoNotPerform() {
		return doNotPerform;
	}

	public String getStatus() {
		return status;
	}

	public Date getAuthoredOn() {
		return authoredOn;
	}

	public FHIRReference getSubject() {
		return subject;
	}

	public FHIRCodeableConcept getCode() {
		return code;
	}

}
