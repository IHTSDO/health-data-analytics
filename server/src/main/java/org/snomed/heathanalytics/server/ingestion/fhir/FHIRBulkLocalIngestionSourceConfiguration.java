package org.snomed.heathanalytics.server.ingestion.fhir;

import org.snomed.heathanalytics.server.ingestion.HealthDataIngestionSourceConfiguration;

import java.io.File;

public class FHIRBulkLocalIngestionSourceConfiguration implements HealthDataIngestionSourceConfiguration {

	private final File patientFile;
	private final File conditionFile;
	private final File procedureFile;

	public FHIRBulkLocalIngestionSourceConfiguration(File patientFile, File conditionFile, File procedureFile) {
		this.patientFile = patientFile;
		this.conditionFile = conditionFile;
		this.procedureFile = procedureFile;
	}

	public File getPatientFile() {
		return patientFile;
	}

	public File getConditionFile() {
		return conditionFile;
	}

	public File getProcedureFile() {
		return procedureFile;
	}
}
