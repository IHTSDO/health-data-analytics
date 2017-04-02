package org.snomed.heathanalytics.rest;

import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.service.QueryService;
import org.snomed.heathanalytics.service.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class CohortController {

	@Autowired
	private QueryService queryService;

	@RequestMapping(value = "/cohort", method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public Page<Patient> fetchCohort(@RequestParam String ecl) throws ServiceException {
		return new Page<>(queryService.fetchCohort(ecl));
	}

}
