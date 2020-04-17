package org.snomed.heathanalytics.service;

import org.ihtsdo.otf.sqs.service.SnomedQueryService;
import org.ihtsdo.otf.sqs.service.dto.ConceptResult;
import org.ihtsdo.otf.sqs.service.dto.ConceptResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SnomedService {

	@Autowired
	private SnomedQueryService snomedQueryService;

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

	public void setSnomedQueryService(SnomedQueryService snomedQueryService) {
		this.snomedQueryService = snomedQueryService;
	}

}
