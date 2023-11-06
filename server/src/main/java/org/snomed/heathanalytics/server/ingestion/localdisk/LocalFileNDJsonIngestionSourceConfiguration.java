package org.snomed.heathanalytics.server.ingestion.localdisk;

import org.snomed.heathanalytics.server.ingestion.HealthDataIngestionSourceConfiguration;

import java.io.File;

public class LocalFileNDJsonIngestionSourceConfiguration implements HealthDataIngestionSourceConfiguration {

	private final File fileDirectory;
	private final String dataset;

	public LocalFileNDJsonIngestionSourceConfiguration(String dataset, File fileDirectory) {
		this.dataset = dataset;
		this.fileDirectory = fileDirectory;
	}

	public File getFileDirectory() {
		return fileDirectory;
	}

	@Override
	public String getDataset() {
		return dataset;
	}
}
