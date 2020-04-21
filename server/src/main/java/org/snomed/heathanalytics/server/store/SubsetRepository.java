package org.snomed.heathanalytics.server.store;

import org.snomed.heathanalytics.server.model.Subset;
import org.springframework.data.elasticsearch.repository.ElasticsearchCrudRepository;

public interface SubsetRepository extends ElasticsearchCrudRepository<Subset, String> {
}
