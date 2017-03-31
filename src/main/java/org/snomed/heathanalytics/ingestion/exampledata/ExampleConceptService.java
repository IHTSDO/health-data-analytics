package org.snomed.heathanalytics.ingestion.exampledata;

import org.ihtsdo.otf.sqs.service.SnomedQueryService;
import org.ihtsdo.otf.sqs.service.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class ExampleConceptService {

	private SnomedQueryService snomedQueryService;

	private final Map<Long, List<Long>> conceptDescendantMap;

	public ExampleConceptService(SnomedQueryService snomedQueryService) {
		this.snomedQueryService = snomedQueryService;
		conceptDescendantMap = new HashMap<>();
	}

	String selectRandomChildOf(String conceptId) throws ServiceException {
		return selectRandomChildOf(Long.parseLong(conceptId)).toString();
	}

	Long selectRandomChildOf(Long conceptId) throws ServiceException {
		List<Long> descendants;
		try {
			descendants = getDescendants(conceptId);
		} catch (ServiceException e) {
			throw new ServiceException("Failed to fetch descendants of " + conceptId, e);
		}
		if (descendants.isEmpty()) {
			return null;
		}
		return descendants.get(ThreadLocalRandom.current().nextInt(0, descendants.size()));
	}

	private List<Long> getDescendants(Long conceptId) throws ServiceException {
		if (!conceptDescendantMap.containsKey(conceptId)) {
			synchronized (conceptDescendantMap) {
				if (!conceptDescendantMap.containsKey(conceptId)) {
					conceptDescendantMap.put(conceptId,
							snomedQueryService.eclQueryReturnConceptIdentifiers("<" + conceptId, 0, 1000).getConceptIds());
				}
			}
		}
		return conceptDescendantMap.get(conceptId);
	}
}
