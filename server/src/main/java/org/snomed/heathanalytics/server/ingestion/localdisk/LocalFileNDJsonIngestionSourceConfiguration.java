package org.snomed.heathanalytics.server.ingestion.localdisk;

import org.snomed.heathanalytics.server.ingestion.HealthDataIngestionSourceConfiguration;

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
