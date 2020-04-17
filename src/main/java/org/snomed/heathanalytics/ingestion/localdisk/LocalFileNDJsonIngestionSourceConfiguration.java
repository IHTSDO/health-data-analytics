package org.snomed.heathanalytics.ingestion.localdisk;

import org.snomed.heathanalytics.ingestion.HealthDataIngestionSourceConfiguration;

import java.io.File;

public class LocalFileNDJsonIngestionSourceConfiguration implements HealthDataIngestionSourceConfiguration {

	private final File fileDirectory;

	public LocalFileNDJsonIngestionSourceConfiguration(File fileDirectory) {
		this.fileDirectory = fileDirectory;
	}

	public File getFileDirectory() {
		return fileDirectory;
	}
}
