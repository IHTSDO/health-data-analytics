package org.snomed.heathanalytics.store;

import org.snomed.heathanalytics.domain.Subset;
import org.springframework.data.elasticsearch.repository.ElasticsearchCrudRepository;

public interface SubsetRepository extends ElasticsearchCrudRepository<Subset, String> {
}
