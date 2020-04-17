package org.snomed.heathanalytics.rest;

import io.swagger.annotations.Api;
import org.ihtsdo.otf.sqs.service.dto.ConceptResult;
import org.ihtsdo.otf.sqs.service.dto.ConceptResults;
import org.snomed.heathanalytics.service.ServiceException;
import org.snomed.heathanalytics.service.SnomedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Api(tags = "Concepts", description = "-")
public class ConceptController {

	@Autowired
	private SnomedService snomedService;

	@RequestMapping(value = "/concepts", method = RequestMethod.GET, produces = "application/json")
	public ConceptResults findConcepts(@RequestParam(required = false) String prefix, @RequestParam(required = false) String ecQuery,
									   @RequestParam(required = false, defaultValue = "20") int limit) throws ServiceException {
		return snomedService.findConcepts(prefix, ecQuery, 0, limit);
	}

	@RequestMapping(value = "/concepts/{conceptId}", method = RequestMethod.GET, produces = "application/json")
	public ConceptResult findConcepts(@PathVariable String conceptId) throws ServiceException {
		return snomedService.findConcept(conceptId);
	}

}
