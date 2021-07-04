package org.snomed.heathanalytics.server.service.stats;

import it.unimi.dsi.fastutil.longs.Long2LongArrayMap;

import java.util.Map;

public class EncounterFrequencyDistribution {

	private Long patientCount = 0L;
	private final Map<Long, Long> conceptCounts = new Long2LongArrayMap();

	public void addPatient() {
		patientCount++;
	}

	public void addEncounter(Long conceptId) {
		Long count = conceptCounts.get(conceptId);
		if (count == null) {
			count = 0L;
		}
		conceptCounts.put(conceptId, ++count);
	}

	public Long getPatientCount() {
		return patientCount;
	}

	public Map<Long, Long> getConceptCounts() {
		return conceptCounts;
	}
}
