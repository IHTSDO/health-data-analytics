package org.snomed.heathanalytics.server.ingestion.fhir;

import java.util.Collections;
import java.util.List;

public class FHIRCodeableConcept {

	public static final String SNOMEDCT_URI = "http://snomed.info/sct";
	public static final FHIRCodeableConcept CLINICAL_STATUS_ACTIVE =
			new FHIRCodeableConcept(Collections.singletonList(new FHIRCoding("http://terminology.hl7.org/CodeSystem/condition-clinical", "active")));
	public static final FHIRCodeableConcept VERIFICATION_STATUS_CONFIRMED =
			new FHIRCodeableConcept(Collections.singletonList(new FHIRCoding("http://terminology.hl7.org/CodeSystem/condition-ver-status", "confirmed")));

	private List<FHIRCoding> coding;

	public FHIRCodeableConcept() {
	}

	public FHIRCodeableConcept(List<FHIRCoding> coding) {
		this.coding = coding;
	}

	public static FHIRCodeableConcept snomedConcept(String conceptId) {
		return new FHIRCodeableConcept(Collections.singletonList(new FHIRCoding(SNOMEDCT_URI, conceptId)));
	}

	public List<FHIRCoding> getCoding() {
		return coding;
	}
}
