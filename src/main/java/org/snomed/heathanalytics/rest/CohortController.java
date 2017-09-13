package org.snomed.heathanalytics.rest;

import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.domain.CohortCriteria;
import org.snomed.heathanalytics.service.QueryService;
import org.snomed.heathanalytics.service.ServiceException;
import org.snomed.heathanalytics.store.CohortCriteriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.snomed.heathanalytics.rest.ControllerHelper.getCreatedResponse;
import static org.snomed.heathanalytics.rest.ControllerHelper.throwIfNotFound;

@RestController
public class CohortController {

	private static final PageRequest LARGE_PAGE = new PageRequest(0, 1000);

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
		criteriaRepository.save(cohortCriteria);
		return getCreatedResponse(cohortCriteria.getId());
	}

	@RequestMapping(value = "/cohorts/{cohortId}", method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public CohortCriteria getCohort(@PathVariable String cohortId) {
		CohortCriteria criteria = criteriaRepository.findOne(cohortId);
		throwIfNotFound(criteria, "Cohort");
		return criteria;
	}

	@RequestMapping(value = "/cohorts/select", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Page<Patient> runCohortSelection(@RequestBody CohortCriteria cohortCriteria,
											@RequestParam(required = false, defaultValue = "0") int page,
											@RequestParam(required = false, defaultValue = "100") int size) throws ServiceException {
		return queryService.fetchCohort(cohortCriteria, page, size);
	}

}
