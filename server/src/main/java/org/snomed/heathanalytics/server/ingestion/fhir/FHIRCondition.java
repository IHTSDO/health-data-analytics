package org.snomed.heathanalytics.server.ingestion.fhir;

import org.elasticsearch.common.Strings;

import java.util.Date;

public class FHIRCondition {

	private FHIRReference subject;
	private FHIRCodeableConcept code;
	private FHIRCodeableConcept clinicalStatus;
	private FHIRCodeableConcept verificationStatus;
	private Date onsetDateTime;

	public boolean isConfirmedActive() {
		if (clinicalStatus != null) {
			if (!clinicalStatus.getCoding().isEmpty()) {
				FHIRCoding status = clinicalStatus.getCoding().iterator().next();
				if ("http://terminology.hl7.org/CodeSystem/condition-clinical".equals(status.getSystem())) {
					String code = status.getCode();
					if (code != null) {
						if (!code.equals("active") && !code.equals("recurrence") && !code.equals("relapse")) {
							return false;
						}
					}
				}
			}
		}
		if (verificationStatus != null) {
			if (!verificationStatus.getCoding().isEmpty()) {
				FHIRCoding status = verificationStatus.getCoding().iterator().next();
				if ("http://terminology.hl7.org/CodeSystem/condition-ver-status".equals(status.getSystem())) {
					String code = status.getCode();
					if (code != null) {
						if (!code.equals("confirmed")) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	public FHIRCodeableConcept getCode() {
		return code;
	}

	public Date getOnsetDateTime() {
		return onsetDateTime;
	}

	public FHIRReference getSubject() {
		return subject;
	}

	public FHIRCodeableConcept getClinicalStatus() {
		return clinicalStatus;
	}

	public FHIRCodeableConcept getVerificationStatus() {
		return verificationStatus;
	}
}
