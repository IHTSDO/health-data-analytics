package org.snomed.heathanalytics.server.store;

import org.snomed.heathanalytics.model.Patient;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface PatientRepository extends ElasticsearchRepository<Patient, String> {
}
