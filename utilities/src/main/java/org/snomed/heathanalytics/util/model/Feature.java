package org.snomed.heathanalytics.util.model;

import java.util.Collections;
import java.util.Set;

public class Feature {

	private final boolean cluster;
	private final Long conceptId;
	private final Long aggregatedFrequency;
	private final Set<Long> weakConcepts;
	private final Set<Long> subFeatures;

	private Feature(boolean cluster, Long conceptId, Long aggregatedFrequency, Set<Long> weakConcepts, Set<Long> subFeatures) {
		this.cluster = cluster;
		this.conceptId = conceptId;
		this.aggregatedFrequency = aggregatedFrequency;
		this.weakConcepts = weakConcepts;
		this.subFeatures = subFeatures;
	}

	public static Feature newCluster(Long conceptId, Long aggregatedFrequency, Set<Long> weakConcepts, Set<Long> subFeatures) {
		return new Feature(true, conceptId, aggregatedFrequency, weakConcepts, subFeatures);
	}

	public static Feature newSurvivingFeature(Long conceptId, Long frequency) {
		return new Feature(false, conceptId, frequency, Collections.emptySet(), Collections.emptySet());
	}

	public boolean isCluster() {
		return cluster;
	}

	public Long getConceptId() {
		return conceptId;
	}

	public Long getAggregatedFrequency() {
		return aggregatedFrequency;
	}

	public Set<Long> getWeakConcepts() {
		return weakConcepts;
	}

	public Set<Long> getSubFeatures() {
		return subFeatures;
	}
}
