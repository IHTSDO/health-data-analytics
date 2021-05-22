package org.snomed.heathanalytics.server.ingestion.fhir;

import java.util.Date;

public class FHIRMedicationRequest {

	private FHIRReference subject;
	private FHIRCodeableConcept medicationCodeableConcept;
	private String status;
	private String intent;
	private Date authoredOn;

	public FHIRMedicationRequest() {
	}

	public FHIRMedicationRequest(FHIRReference subject, FHIRCodeableConcept medicationCodeableConcept, String status, String intent, Date authoredOn) {
		this.subject = subject;
		this.medicationCodeableConcept = medicationCodeableConcept;
		this.status = status;
		this.intent = intent;
		this.authoredOn = authoredOn;
	}

	public String getResourceType() {
		return "MedicationRequest";
	}

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
