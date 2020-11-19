package org.snomed.heathanalytics.server.ingestion.fhir;

import org.snomed.heathanalytics.model.Gender;
import org.snomed.heathanalytics.server.ingestion.HealthDataIngestionSourceConfiguration;

import java.io.File;

public class FHIRLocalIngestionSourceConfiguration implements HealthDataIngestionSourceConfiguration {

	public enum FHIR_VERSION {dstu3, r4};

	private final File fileDirectory;
	private FHIR_VERSION fhirVersion = FHIR_VERSION.r4;

	public FHIRLocalIngestionSourceConfiguration(File fileDirectory, String fhirVersion) {
		this.fileDirectory = fileDirectory;
		for (FHIR_VERSION fhir_version : FHIR_VERSION.values()) {
			if (fhir_version.name().equalsIgnoreCase(fhirVersion)) {
				this.fhirVersion = fhir_version;
				break;
			}
		}
	}

	public File getFileDirectory() {
		return fileDirectory;
	}

	public FHIR_VERSION getFhirVersion() {
		return fhirVersion;
	}
}
