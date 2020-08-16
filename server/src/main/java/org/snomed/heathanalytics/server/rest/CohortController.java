package org.snomed.heathanalytics.server.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.snomed.heathanalytics.model.ClinicalEncounter;
import org.snomed.heathanalytics.server.model.CohortCriteria;
import org.snomed.heathanalytics.model.Patient;
import org.snomed.heathanalytics.server.service.QueryService;
import org.snomed.heathanalytics.server.service.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.TreeSet;

@RestController
@Api(tags = "Patients", description = "-")
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
		Page<Patient> patients = queryService.fetchCohort(cohortCriteria, page, size);
		for (Patient patient : patients.getContent()) {
			TreeSet<ClinicalEncounter> encounters = new TreeSet<>(Comparator.comparing(ClinicalEncounter::getDate).thenComparing(ClinicalEncounter::getConceptId));
			encounters.addAll(patient.getEncounters());
			patient.setEncounters(encounters);
		}
		return patients;
	}

}
