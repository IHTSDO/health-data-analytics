package org.snomed.heathanalytics.ingestion.exampledata;

import org.snomed.heathanalytics.snomed.SnomedSubsumptionService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class ExampleConceptService {

	private final SnomedSubsumptionService snomedSubsumptionService;
	private final Map<Long, List<Long>> conceptDescendantMap;

	public ExampleConceptService(SnomedSubsumptionService snomedSubsumptionService) {
		this.snomedSubsumptionService = snomedSubsumptionService;
		conceptDescendantMap = new HashMap<>();
	}

	String selectRandomChildOf(String conceptId) {
		return selectRandomChildOf(Long.parseLong(conceptId)).toString();
	}

	Long selectRandomChildOf(Long conceptId) {
		List<Long> descendants = getDescendants(conceptId);
		if (descendants.isEmpty()) {
			return null;
		}
		return descendants.get(ThreadLocalRandom.current().nextInt(0, descendants.size()));
	}

	private List<Long> getDescendants(Long conceptId) {
		if (!conceptDescendantMap.containsKey(conceptId)) {
			synchronized (conceptDescendantMap) {
				if (!conceptDescendantMap.containsKey(conceptId)) {
					conceptDescendantMap.put(conceptId, new ArrayList<>(snomedSubsumptionService.getDescendantsOf(conceptId)));
				}
			}
		}
		return conceptDescendantMap.get(conceptId);
	}
}
