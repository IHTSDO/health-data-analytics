package org.snomed.heathanalytics.server.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.snomed.heathanalytics.model.ClinicalEvent;
import org.snomed.heathanalytics.model.Patient;
import org.snomed.heathanalytics.server.model.CohortCriteria;
import org.snomed.heathanalytics.server.service.PatientQueryService;
import org.snomed.heathanalytics.server.service.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

@RestController
@RequestMapping("/api")
@Tag(name = "Patients", description = "-")
public class CohortController {

	@Autowired
	private PatientQueryService patientQueryService;

	@Operation(summary = "Retrieve patients using cohort criteria",
			description = "Retrieve patients which match the given cohort criteria. " +
			"Within additionalCriteria a days value of '-1' can be used as an unbounded value.")
	@RequestMapping(value = "/cohorts/select", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Page<Patient> runCohortSelection(@RequestBody CohortCriteria cohortCriteria,
											@RequestParam(required = false, defaultValue = "0") int page,
											@RequestParam(required = false, defaultValue = "100") int size) throws ServiceException {
		Page<Patient> patients = patientQueryService.fetchCohort(cohortCriteria, page, size);
		for (Patient patient : patients.getContent()) {
			TreeSet<ClinicalEvent> events = new TreeSet<>(Comparator.comparing(ClinicalEvent::getDate).thenComparing(ClinicalEvent::getConceptId));
			events.addAll(patient.getEvents());
			patient.setEvents(events);
		}
		return patients;
	}

	@RequestMapping(value = "/datasets", method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public List<String> getDatasets() {
		return patientQueryService.getDatasets();
	}

}
