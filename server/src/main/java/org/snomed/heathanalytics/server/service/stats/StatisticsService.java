package org.snomed.heathanalytics.server.service.stats;

import com.google.common.collect.Sets;
import org.snomed.heathanalytics.server.service.ServiceException;
import org.snomed.heathanalytics.server.service.SnomedService;
import org.snomed.heathanalytics.server.service.util.MapUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

	@Autowired
	private SnomedService snomedService;

	public Map<Long, Long> fillAndSortFrequencyDistribution(Map<Long, Long> conceptCounts) throws ServiceException {
		for (Long concept : new ArrayList<>(conceptCounts.keySet())) {
			Set<Long> conceptAncestors = snomedService.getConceptAncestors(concept.toString());
			fillAncestors(concept, conceptAncestors, conceptCounts);
		}
		Set<Long> tooSmall = conceptCounts.entrySet().stream().filter((entry) -> entry.getValue() < 10).map(Map.Entry::getKey).collect(Collectors.toSet());
		for (Long conceptId : tooSmall) {
			conceptCounts.remove(conceptId);
		}
		return MapUtil.sortByValue(conceptCounts);
	}

	public EncounterRatioFrequencyDistribution getMatrixDiff(EncounterFrequencyDistribution leftMatrix, EncounterFrequencyDistribution rightMatrix) throws ServiceException {
		Map<Long, Long> leftConceptCounts = leftMatrix.getConceptCounts();
		Map<Long, Long> rightConceptCounts = rightMatrix.getConceptCounts();
		for (Long encounterConceptId : Sets.union(leftMatrix.getConceptCounts().keySet(), rightMatrix.getConceptCounts().keySet())) {
			Set<Long> ancestors = snomedService.getConceptAncestors(encounterConceptId.toString());
			fillAncestors(encounterConceptId, ancestors, leftConceptCounts);
			fillAncestors(encounterConceptId, ancestors, rightConceptCounts);
		}
		return new EncounterRatioFrequencyDistribution(leftMatrix, rightMatrix);
	}

	private void fillAncestors(Long encounterConceptId, Set<Long> ancestors, Map<Long, Long> matrixCounts) {
		if (matrixCounts.containsKey(encounterConceptId)) {
			Long count = matrixCounts.get(encounterConceptId);
			for (Long ancestor : ancestors) {
				Long ancestorCount = matrixCounts.get(ancestor);
				if (ancestorCount == null) {
					ancestorCount = 0L;
				}
				ancestorCount += count;
				matrixCounts.put(ancestor, ancestorCount);
			}
		}
	}

}
