package org.snomed.heathanalytics.ingestion;

public interface HealthDataIngestionSource {

	void stream(HealthDataIngestionSourceConfiguration configuration, HealthDataOutputStream healthDataOutputStream);

}
