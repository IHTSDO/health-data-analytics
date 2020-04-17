package org.snomed.heathanalytics.rest;

import io.swagger.annotations.ApiOperation;
import org.snomed.heathanalytics.domain.Report;
import org.snomed.heathanalytics.domain.ReportDefinition;
import org.snomed.heathanalytics.domain.StatisticalCorrelationReport;
import org.snomed.heathanalytics.domain.StatisticalCorrelationReportDefinition;
import org.snomed.heathanalytics.service.ReportService;
import org.snomed.heathanalytics.service.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReportController {

	@Autowired
	private ReportService reportService;

	@ApiOperation(value = "Create a report of patient counts using groups.",
			notes = "A top level criteria can be defined for the overall cohort.\n" +
					"Groups with more specific criteria can also be defined if required.\n" +
					"A count of matching patients will be returned for the top level and each specified group.\n" +
					"The top level criteria will be included in each group automatically so there is no need to repeat the top level criteria.\n" +
					"\n" +
					"In the report request the groups section is a list of lists. If a second list of groups is given " +
					"these will become subgroups within each of the first list of groups.\n" +
					"For example if the groups in the request are [Smoker, Non-Smoker][Foot Amputation]\n" +
					"the groups in the results will be: [Smoker, [(Smoker +) Foot Amputation]], [Non-Smoker, [(Non-Smoker +) Foot Amputation]].\n" +
					"The criteria of top level groups is inherited by subgroups." +
					"\n" +
					"Within encounterCriteria days value of '-1' can be used as an unbounded value.")
	@RequestMapping(value = "/report", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Report runReport(@RequestBody ReportDefinition reportDefinition) throws ServiceException {
		return reportService.runReport(reportDefinition);
	}

	@ApiOperation(value = "Statistical encounter correlation report.",
			notes = "Within encounterCriteria days value of '-1' can be used as an unbounded value.")
	@RequestMapping(value = "/statistical-correlation-report", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public StatisticalCorrelationReport runReportSta(@RequestBody StatisticalCorrelationReportDefinition reportDefinition) throws ServiceException {
		return reportService.runStatisticalReport(reportDefinition);
	}

}
