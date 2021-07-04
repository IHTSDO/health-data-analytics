package org.snomed.heathanalytics.server.model;

import org.snomed.heathanalytics.model.Patient;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public class PatientPageWithEncounterFrequencyDistribution extends PageImpl<Patient> {

	private Map<Long, Long> conceptCounts;

	public PatientPageWithEncounterFrequencyDistribution(List<Patient> content, Pageable pageable, long total, Map<Long, Long> conceptCounts) {
		super(content, pageable, total);
		this.conceptCounts = conceptCounts;
	}

	public Map<Long, Long> getConceptCounts() {
		return conceptCounts;
	}

	public void setConceptCounts(Map<Long, Long> conceptCounts) {
		this.conceptCounts = conceptCounts;
	}
}
