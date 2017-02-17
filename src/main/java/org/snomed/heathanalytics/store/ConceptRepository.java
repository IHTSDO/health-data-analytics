package org.snomed.heathanalytics.store;

import org.snomed.heathanalytics.snomed.Concept;
import org.springframework.data.elasticsearch.repository.ElasticsearchCrudRepository;

public interface ConceptRepository extends ElasticsearchCrudRepository<Concept, String> {
}
