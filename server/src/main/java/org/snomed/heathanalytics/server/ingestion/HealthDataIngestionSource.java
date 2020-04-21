package org.snomed.heathanalytics.server.ingestion;

public interface HealthDataIngestionSource {

	void stream(HealthDataIngestionSourceConfiguration configuration, HealthDataOutputStream healthDataOutputStream);

}
