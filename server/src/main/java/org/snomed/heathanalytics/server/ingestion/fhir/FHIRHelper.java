package org.snomed.heathanalytics.server.ingestion.fhir;

import org.elasticsearch.common.Strings;

public class FHIRHelper {
	public static String getSubjectId(FHIRReference subject) {
		if (subject != null) {
			String reference = subject.getReference();
			if (!Strings.isNullOrEmpty(reference) && reference.startsWith("Patient/")) {
				return reference.substring(8);
			}
		}
		return null;
	}
}
