package org.snomed.heathanalytics.server.ingestion.fhir;

import org.snomed.heathanalytics.server.ingestion.HealthDataIngestionSourceConfiguration;

import java.io.File;

public class FHIRBulkLocalIngestionSourceConfiguration implements HealthDataIngestionSourceConfiguration {

	private final String dataset;
	private final File patientFile;
	private final File conditionFile;
	private final File procedureFile;
	private final File medicationRequestFile;
	private final File serviceRequestFile;

	public FHIRBulkLocalIngestionSourceConfiguration(String dataset, File patientFile, File conditionFile, File procedureFile, File medicationRequestFile,
			File serviceRequestFile) {
		this.dataset = dataset;
		this.patientFile = patientFile;
		this.conditionFile = conditionFile;
		this.procedureFile = procedureFile;
		this.medicationRequestFile = medicationRequestFile;
		this.serviceRequestFile = serviceRequestFile;
	}

	@Override
	public String getDataset() {
		return dataset;
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

	public File getServiceRequestFile() {
		return serviceRequestFile;
	}
}
