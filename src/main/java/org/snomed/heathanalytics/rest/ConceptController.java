package org.snomed.heathanalytics.rest;

import org.ihtsdo.otf.sqs.service.dto.ConceptResult;
import org.ihtsdo.otf.sqs.service.dto.ConceptResults;
import org.snomed.heathanalytics.service.QueryService;
import org.snomed.heathanalytics.service.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class ConceptController {

	@Autowired
	private QueryService queryService;

	@RequestMapping(value = "/concepts", method = RequestMethod.GET, produces = "application/json")
	public ConceptResults findConcepts(@RequestParam(required = false) String prefix, @RequestParam(required = false) String ecQuery,
									   @RequestParam(required = false, defaultValue = "20") int limit) throws ServiceException {
		return queryService.findConcepts(prefix, ecQuery, 0, limit);
	}

	@RequestMapping(value = "/concepts/{conceptId}", method = RequestMethod.GET, produces = "application/json")
	public ConceptResult findConcepts(@PathVariable String conceptId) throws ServiceException {
		return queryService.findConcept(conceptId);
	}

}
