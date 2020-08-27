package org.snomed.heathanalytics.server.ingestion;

public interface HealthDataIngestionSource {

	int ES_WRITE_BATCH_SIZE = 1_000;

	void stream(HealthDataIngestionSourceConfiguration configuration, HealthDataOutputStream healthDataOutputStream);

}
