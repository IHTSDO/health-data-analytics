package org.snomed.heathanalytics.rest;

import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.domain.CohortCriteria;
import org.snomed.heathanalytics.service.Criterion;
import org.snomed.heathanalytics.service.QueryService;
import org.snomed.heathanalytics.service.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class CohortController {

	@Autowired
	private QueryService queryService;

	@RequestMapping(value = "/cohort", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public org.springframework.data.domain.Page<Patient> fetchCohort(@RequestBody CohortCriteria cohortCriteria) throws ServiceException {
		return queryService.fetchCohort(cohortCriteria);
	}

}
