package org.snomed.heathanalytics.server.rest;

import io.swagger.annotations.Api;
import org.snomed.heathanalytics.server.pojo.ConceptResult;
import org.snomed.heathanalytics.server.service.ServiceException;
import org.snomed.heathanalytics.server.service.SnomedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Api(tags = "Concepts", description = "-")
public class ConceptController {

	@Autowired
	private SnomedService snomedService;

	@RequestMapping(value = "/concepts", method = RequestMethod.GET, produces = "application/json")
	public List<ConceptResult> findConcepts(@RequestParam(required = false) String prefix, @RequestParam(required = false) String ecl,
									   @RequestParam(required = false, defaultValue = "20") int limit) throws ServiceException {

		return snomedService.findConcepts(ecl, prefix, 0, limit);
	}

	@RequestMapping(value = "/concepts/{conceptId}", method = RequestMethod.GET, produces = "application/json")
	public ConceptResult findConcepts(@PathVariable String conceptId) throws ServiceException {
		return snomedService.findConcept(conceptId);
	}

}
