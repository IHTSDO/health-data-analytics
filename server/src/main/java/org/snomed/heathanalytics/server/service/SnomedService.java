package org.snomed.heathanalytics.server.service;

import org.ihtsdo.otf.sqs.service.SnomedQueryService;
import org.ihtsdo.otf.sqs.service.dto.ConceptResult;
import org.ihtsdo.otf.sqs.service.dto.ConceptResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service
public class SnomedService {

	@Autowired
	private SnomedQueryService snomedQueryService;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public ConceptResult findConcept(String conceptId) throws ServiceException {
		try {
			return snomedQueryService.retrieveConcept(conceptId);
		} catch (org.ihtsdo.otf.sqs.service.exception.ServiceException e) {
			throw new ServiceException("Failed to find concept by id '" + conceptId + "'", e);
		}
	}

	public ConceptResults findConcepts(String termPrefix, String ecQuery, int offset, int limit) throws ServiceException {
		try {
			return snomedQueryService.search(ecQuery, termPrefix, offset, limit);
		} catch (org.ihtsdo.otf.sqs.service.exception.ServiceException e) {
			throw new ServiceException("Failed to find concept by prefix '" + termPrefix + "'", e);
		}
	}

	public List<Long> getConceptIds(String ecl) throws ServiceException {
		try {
			return snomedQueryService.eclQueryReturnConceptIdentifiers(ecl, 0, -1).getConceptIds();
		} catch (org.ihtsdo.otf.sqs.service.exception.ServiceException e) {
			throw new ServiceException("Failed to process ECL query.", e);
		}
	}

	public Set<Long> getConceptAncestors(String conceptId) throws ServiceException {
		try {
			ConceptResults conceptResults = snomedQueryService.retrieveConceptAncestors(conceptId);
			if (conceptResults != null) {
				return conceptResults.getItems().stream().map(ConceptResult::getId).map(Long::parseLong).collect(Collectors.toSet());
			}
			return Collections.emptySet();
		} catch (org.ihtsdo.otf.sqs.service.exception.ServiceException e) {
			logger.warn(format("Failed to fetch concept ancestors for '%s'.", conceptId));
			return Collections.emptySet();
		}
	}

	public void setSnomedQueryService(SnomedQueryService snomedQueryService) {
		this.snomedQueryService = snomedQueryService;
	}

}
