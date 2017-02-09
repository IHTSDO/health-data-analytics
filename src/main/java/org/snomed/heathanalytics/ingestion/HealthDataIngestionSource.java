package org.snomed.heathanalytics.ingestion;

public interface HealthDataIngestionSource {

	void stream(HealthDataOutputStream healthDataOutputStream);

}
