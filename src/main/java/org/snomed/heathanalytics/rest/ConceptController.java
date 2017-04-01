package org.snomed.heathanalytics.rest;

import org.ihtsdo.otf.sqs.service.dto.ConceptResults;
import org.snomed.heathanalytics.service.QueryService;
import org.snomed.heathanalytics.service.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConceptController {

	@Autowired
	private QueryService queryService;

	@RequestMapping(value = "/concepts", method = RequestMethod.GET, produces = "application/json")
	public ConceptResults findConcepts(@RequestParam String prefix, @RequestParam(required = false, defaultValue = "20") int limit) throws ServiceException {
		return queryService.findConcepts(prefix, 0, limit);
	}

}
