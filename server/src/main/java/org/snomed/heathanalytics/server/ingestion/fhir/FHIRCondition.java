package org.snomed.heathanalytics.server.ingestion.fhir;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.elasticsearch.common.Strings;

import java.util.Date;

public class FHIRCondition {

	private FHIRReference subject;
	private FHIRCodeableConcept code;
	private FHIRCodeableConcept clinicalStatus;
	private FHIRCodeableConcept verificationStatus;
	private Date onsetDateTime;

	public FHIRCondition() {
	}

	public FHIRCondition(FHIRReference subject, FHIRCodeableConcept code, FHIRCodeableConcept clinicalStatus, FHIRCodeableConcept verificationStatus, Date onsetDateTime) {
		this.subject = subject;
		this.code = code;
		this.clinicalStatus = clinicalStatus;
		this.verificationStatus = verificationStatus;
		this.onsetDateTime = onsetDateTime;
	}

	public String getResourceType() {
		return "Condition";
	}

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

	// 1982-10-26T09:38:35+00:00
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
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
