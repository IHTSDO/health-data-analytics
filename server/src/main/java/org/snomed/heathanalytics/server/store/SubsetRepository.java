package org.snomed.heathanalytics.server.store;

import org.snomed.heathanalytics.server.model.Subset;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface SubsetRepository extends ElasticsearchRepository<Subset, String> {
}
