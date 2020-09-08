package org.snomed.heathanalytics.server.ingestion.fhir;

import org.snomed.heathanalytics.server.ingestion.HealthDataIngestionSourceConfiguration;

import java.io.File;

public class FHIRBulkLocalIngestionSourceConfiguration implements HealthDataIngestionSourceConfiguration {

	private final File patientFile;
	private final File conditionFile;

	public FHIRBulkLocalIngestionSourceConfiguration(File patientFile, File conditionFile) {
		this.patientFile = patientFile;
		this.conditionFile = conditionFile;
	}

	public File getPatientFile() {
		return patientFile;
	}

	public File getConditionFile() {
		return conditionFile;
	}
}
