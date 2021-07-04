package org.snomed.heathanalytics.server.service.stats;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.longs.Long2FloatArrayMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class EncounterRatioFrequencyDistribution {

	private final Map<Long, Float> conceptFractions = new Long2FloatArrayMap();

	private static final Logger logger = LoggerFactory.getLogger(EncounterRatioFrequencyDistribution.class);

	public EncounterRatioFrequencyDistribution(EncounterFrequencyDistribution leftMatrix, EncounterFrequencyDistribution rightMatrix) {
		if (leftMatrix.getPatientCount() == 0 || rightMatrix.getPatientCount() == 0) {
			return;
		}
		Map<Long, Long> leftConceptCounts = leftMatrix.getConceptCounts();
		Map<Long, Long> rightConceptCounts = rightMatrix.getConceptCounts();
		for (Long conceptId : Sets.union(leftConceptCounts.keySet(), rightConceptCounts.keySet())) {
			add(conceptId, leftConceptCounts.get(conceptId), leftMatrix.getPatientCount(), rightConceptCounts.get(conceptId), rightMatrix.getPatientCount());
		}
	}

	private void add(Long concept, Long leftConceptCount, Long leftPatientCount, Long rightConceptCount, Long rightPatientCount) {
		if (leftConceptCount == null) {
			leftConceptCount = 0L;
		}
		if (rightConceptCount == null) {
			rightConceptCount = 0L;
		}

		float normLeftConceptCount = (float)leftConceptCount / (float)leftPatientCount;
		logger.info("leftConceptCount/leftPatientCount : {}/{} = {}", leftConceptCount, leftPatientCount, normLeftConceptCount);

		float normRightConceptCount = (float)rightConceptCount / (float)rightPatientCount;
		logger.info("rightConceptCount/rightPatientCount : {}/{} = {}", rightConceptCount, rightPatientCount, normRightConceptCount);

		float fraction = normLeftConceptCount - normRightConceptCount;
		logger.info("fraction = {}", fraction);

		// Round to two decimal points
		fraction = fraction * 100;
		fraction = Math.round(fraction);
		fraction = fraction / 100;

		conceptFractions.put(concept, fraction);
	}

	public Map<Long, Float> getConceptFractions() {
		return conceptFractions;
	}

	@Override
	public String toString() {
		return conceptFractions.toString();
	}
}
