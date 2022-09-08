package org.snomed.heathanalytics.server.service;

import org.snomed.heathanalytics.server.model.CohortCriteria;
import org.snomed.heathanalytics.server.model.ConceptDifferential;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PatientDataDiscoveryService {

	public List<ConceptDifferential> findCodesDifferentiatingPatientGroupB(CohortCriteria groupA, CohortCriteria groupB) {
		Map<Long, Integer> groupAConceptCounts = getConceptCounts(groupA);
		return null;
	}

	private Map<Long, Integer> getConceptCounts(CohortCriteria group) {
		return null;
	}

}
