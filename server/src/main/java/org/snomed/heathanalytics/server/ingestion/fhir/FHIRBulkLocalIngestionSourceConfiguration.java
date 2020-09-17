package org.snomed.heathanalytics.server.ingestion.fhir;

import org.snomed.heathanalytics.server.ingestion.HealthDataIngestionSourceConfiguration;

import java.io.File;

public class FHIRBulkLocalIngestionSourceConfiguration implements HealthDataIngestionSourceConfiguration {

	private final File patientFile;
	private final File conditionFile;
	private final File procedureFile;
	private final File medicationRequestFile;

	public FHIRBulkLocalIngestionSourceConfiguration(File patientFile, File conditionFile, File procedureFile, File medicationRequestFile) {
		this.patientFile = patientFile;
		this.conditionFile = conditionFile;
		this.procedureFile = procedureFile;
		this.medicationRequestFile = medicationRequestFile;
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

	public File getMedicationRequestFile() {
		return medicationRequestFile;
	}
}
