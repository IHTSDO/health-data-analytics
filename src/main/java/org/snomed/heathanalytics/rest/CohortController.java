package org.snomed.heathanalytics.rest;

import io.swagger.annotations.ApiOperation;
import org.snomed.heathanalytics.domain.CohortCriteria;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.pojo.EmptyPojo;
import org.snomed.heathanalytics.service.InputValidationHelper;
import org.snomed.heathanalytics.service.QueryService;
import org.snomed.heathanalytics.service.ServiceException;
import org.snomed.heathanalytics.service.StatisticalTestResult;
import org.snomed.heathanalytics.store.CohortCriteriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static org.snomed.heathanalytics.rest.ControllerHelper.getCreatedResponse;
import static org.snomed.heathanalytics.rest.ControllerHelper.throwIfNotFound;

@RestController
public class CohortController {

	private static final PageRequest LARGE_PAGE = PageRequest.of(0, 1000);

	@Autowired
	private QueryService queryService;

	@Autowired
	private CohortCriteriaRepository criteriaRepository;

	@RequestMapping(value = "/cohorts", method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public List<CohortCriteria> listCohorts() {
		return criteriaRepository.findAll(LARGE_PAGE).getContent();
	}

	@RequestMapping(value = "/cohorts", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public ResponseEntity<Void> saveCohort(@RequestBody CohortCriteria cohortCriteria) {
		InputValidationHelper.checkInput("Cohort ID must be null or not empty.", cohortCriteria.getId() == null || !cohortCriteria.getId().isEmpty());
		validateSelection(cohortCriteria);
		criteriaRepository.save(cohortCriteria);
		return getCreatedResponse(cohortCriteria.getId());
	}

	@RequestMapping(value = "/cohorts/{cohortId}", method = RequestMethod.PUT, produces = "application/json")
	@ResponseBody
	public CohortCriteria updateCohort(@PathVariable String cohortId, @RequestBody CohortCriteria cohortCriteria) throws ServiceException {
		InputValidationHelper.checkInput("Cohort ID not be empty.",
				cohortId != null && !cohortId.isEmpty());
		InputValidationHelper.checkInput("Cohort ID in path does not match the request body.",
				cohortId.equals(cohortCriteria.getId()));

		return criteriaRepository.save(cohortCriteria);
	}


	@RequestMapping(value = "/cohorts/{cohortId}", method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public CohortCriteria getCohort(@PathVariable String cohortId) {
		Optional<CohortCriteria> criteria = criteriaRepository.findById(cohortId);
		throwIfNotFound(criteria, "Cohort");
		return criteria.get();
	}

	@RequestMapping(value = "/cohorts/{cohortId}", method = RequestMethod.DELETE, produces = "application/json")
	@ResponseBody
	public EmptyPojo deleteCohort(@PathVariable String cohortId) {
		criteriaRepository.deleteById(cohortId);
		return new EmptyPojo(); // This is a workaround for the frontend implementation.
	}

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
		InputValidationHelper.checkInput("There must be a Primary Criterion", cohortCriteria.getPrimaryCriterion() != null);
		InputValidationHelper.checkInput("The Primary Criterion can not be an exclusion.", cohortCriteria.getPrimaryCriterion().isHas());
	}

}
