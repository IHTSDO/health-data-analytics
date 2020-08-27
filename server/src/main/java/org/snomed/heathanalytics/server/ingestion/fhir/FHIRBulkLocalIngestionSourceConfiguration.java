package org.snomed.heathanalytics.server.ingestion.fhir;

import org.snomed.heathanalytics.server.ingestion.HealthDataIngestionSourceConfiguration;

import java.io.File;

public class FHIRBulkLocalIngestionSourceConfiguration implements HealthDataIngestionSourceConfiguration {

	private final File patientFile;

	public FHIRBulkLocalIngestionSourceConfiguration(File patientFile) {
		this.patientFile = patientFile;
	}

	public File getPatientFile() {
		return patientFile;
	}
}
