package org.snomed.heathanalytics.rest;

import io.swagger.annotations.ApiOperation;
import org.snomed.heathanalytics.domain.CohortCriteria;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.service.InputValidationHelper;
import org.snomed.heathanalytics.service.QueryService;
import org.snomed.heathanalytics.service.ServiceException;
import org.snomed.heathanalytics.service.StatisticalTestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
public class CohortController {

	@Autowired
	private QueryService queryService;

	@ApiOperation("Retrieve patients which match the given cohort criteria. " +
			"Within additionalCriteria a days value of '-1' can be used as an unbounded value.")
	@RequestMapping(value = "/cohorts/select", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Page<Patient> runCohortSelection(@RequestBody CohortCriteria cohortCriteria,
											@RequestParam(required = false, defaultValue = "0") int page,
											@RequestParam(required = false, defaultValue = "100") int size) throws ServiceException {
		validateSelection(cohortCriteria);
		return queryService.fetchCohort(cohortCriteria, page, size);
	}

	@RequestMapping(value = "/cohorts/statistical-test", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public StatisticalTestResult runStatisticalTest(@RequestBody CohortCriteria cohortCriteria) throws ServiceException {
		validateSelection(cohortCriteria);
		return queryService.fetchStatisticalTestResult(cohortCriteria);
	}

	@ApiOperation("For development purposes only. Has hardcoded results.")
	@RequestMapping(value = "/cohorts/statistical-test-dev", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public StatisticalTestResult runStatisticalTestDevResults(@RequestBody CohortCriteria cohortCriteria) throws ServiceException {
		return new StatisticalTestResult(21876987, 63, 641, 373, 8832);
	}

	private void validateSelection(@RequestBody CohortCriteria cohortCriteria) {
		InputValidationHelper.checkInput("The Primary Criterion can not be an exclusion.", cohortCriteria.getPrimaryCriterion() == null || cohortCriteria.getPrimaryCriterion().isHas());
	}

}
