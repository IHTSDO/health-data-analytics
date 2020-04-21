package org.snomed.heathanalytics.datageneration;

public class ExampleDataGeneratorConfiguration {

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
