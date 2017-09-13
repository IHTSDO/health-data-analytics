package org.snomed.heathanalytics.store;

import org.snomed.heathanalytics.domain.CohortCriteria;
import org.snomed.heathanalytics.domain.Subset;
import org.springframework.data.elasticsearch.repository.ElasticsearchCrudRepository;

public interface CohortCriteriaRepository extends ElasticsearchCrudRepository<CohortCriteria, String> {
}
