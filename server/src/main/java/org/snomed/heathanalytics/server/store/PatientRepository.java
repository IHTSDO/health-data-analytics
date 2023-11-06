package org.snomed.heathanalytics.server.store;

import org.snomed.heathanalytics.model.Patient;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Optional;

public interface PatientRepository extends ElasticsearchRepository<Patient, String> {

	Optional<Patient> findByRoleIdAndDataset(String id, String dataset);

}
