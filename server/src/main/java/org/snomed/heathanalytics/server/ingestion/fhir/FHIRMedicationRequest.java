package org.snomed.heathanalytics.server.ingestion.fhir;

import java.util.Date;

public class FHIRMedicationRequest {

	private FHIRReference subject;
	private FHIRCodeableConcept medicationCodeableConcept;
	private String status;
	private String intent;
	private Date authoredOn;

	public boolean isActiveOrder() {
		return "active".equals(status) && intent != null && intent.contains("order");
	}

	public FHIRReference getSubject() {
		return subject;
	}

	public FHIRCodeableConcept getMedicationCodeableConcept() {
		return medicationCodeableConcept;
	}

	public String getStatus() {
		return status;
	}

	public String getIntent() {
		return intent;
	}

	public Date getAuthoredOn() {
		return authoredOn;
	}
}
