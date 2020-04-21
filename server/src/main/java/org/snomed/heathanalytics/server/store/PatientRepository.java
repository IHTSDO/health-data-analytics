package org.snomed.heathanalytics.server.store;

import org.snomed.heathanalytics.model.Patient;
import org.springframework.data.elasticsearch.repository.ElasticsearchCrudRepository;

public interface PatientRepository extends ElasticsearchCrudRepository<Patient, String> {
}
