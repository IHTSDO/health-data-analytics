package org.snomed.heathanalytics.ingestion.exampledata;

import org.ihtsdo.otf.sqs.service.SnomedQueryService;
import org.ihtsdo.otf.sqs.service.dto.ConceptResult;
import org.ihtsdo.otf.sqs.service.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Long.parseLong;

public class ExampleConceptService {

	private SnomedQueryService snomedQueryService;

	private final Map<Long, List<Long>> conceptDescendantMap;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public ExampleConceptService(SnomedQueryService snomedQueryService) {
		this.snomedQueryService = snomedQueryService;
		conceptDescendantMap = new HashMap<>();
	}

	Long selectRandomChildOf(String conceptId) throws ServiceException {
		List<Long> descendants;
		try {
			descendants = getDescendants(parseLong(conceptId));
		} catch (ServiceException e) {
			throw new ServiceException("Failed to fetch descendants of " + conceptId, e);
		}
		if (descendants.isEmpty()) {
			throw new ServiceException("Concept " + conceptId + " could not be found.");
		}
		return descendants.get(ThreadLocalRandom.current().nextInt(0, descendants.size()));
	}

	private List<Long> getDescendants(Long conceptId) throws ServiceException {
		if (!conceptDescendantMap.containsKey(conceptId)) {
			synchronized (conceptDescendantMap) {
				if (!conceptDescendantMap.containsKey(conceptId)) {
					List<Long> descendants = snomedQueryService.eclQueryReturnConceptIdentifiers("<" + conceptId, 0, 1000).getConceptIds();
					if (descendants.isEmpty()) {
						ConceptResult conceptResult = snomedQueryService.retrieveConcept(conceptId.toString());
						if (conceptResult == null) {
							throw new ServiceException("Concept " + conceptId + " could not be found.");
						}
						logger.warn("Concept " + conceptId + " has no descendants, using the concept itself.");
						descendants = Collections.singletonList(conceptId);
					}
					conceptDescendantMap.put(conceptId,
							descendants);
				}
			}
		}
		return conceptDescendantMap.get(conceptId);
	}
}
