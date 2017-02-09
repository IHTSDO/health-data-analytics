package org.snomed.heathanalytics.ingestion.store.elasticsearch;

import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.springframework.data.elasticsearch.repository.ElasticsearchCrudRepository;

public interface ClinicalEncounterRepository extends ElasticsearchCrudRepository<ClinicalEncounter, String> {
}
