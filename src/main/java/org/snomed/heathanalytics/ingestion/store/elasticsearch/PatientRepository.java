package org.snomed.heathanalytics.ingestion.store.elasticsearch;

import org.snomed.heathanalytics.domain.Patient;
import org.springframework.data.elasticsearch.repository.ElasticsearchCrudRepository;

public interface PatientRepository extends ElasticsearchCrudRepository<Patient, String> {
}
