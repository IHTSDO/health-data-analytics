package org.snomed.heathanalytics.server.ingestion.fhir;

public class FHIRReference {

	private String reference;

	public FHIRReference() {
	}

	public FHIRReference(String reference) {
		this.reference = reference;
	}

	public static FHIRReference patient(String patientCode) {
		return new FHIRReference("Patient/" + patientCode);
	}

	public String getReference() {
		return reference;
	}
}
