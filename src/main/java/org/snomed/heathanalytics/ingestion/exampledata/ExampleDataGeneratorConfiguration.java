package org.snomed.heathanalytics.ingestion.exampledata;

import org.snomed.heathanalytics.ingestion.HealthDataIngestionSourceConfiguration;

public class ExampleDataGeneratorConfiguration implements HealthDataIngestionSourceConfiguration {

	private int demoPatientCount;

	public ExampleDataGeneratorConfiguration(int demoPatientCount) {
		this.demoPatientCount = demoPatientCount;
	}

	public int getDemoPatientCount() {
		return demoPatientCount;
	}

	public void setDemoPatientCount(int demoPatientCount) {
		this.demoPatientCount = demoPatientCount;
	}
}
