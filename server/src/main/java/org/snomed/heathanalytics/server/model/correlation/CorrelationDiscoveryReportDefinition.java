package org.snomed.heathanalytics.server.model.correlation;

import org.snomed.heathanalytics.server.model.CohortCriteria;

public record CorrelationDiscoveryReportDefinition(CohortCriteria baseCriteria, String negativeOutcomeECL) {

}
