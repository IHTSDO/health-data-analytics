package org.snomed.heathanalytics.server.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.snomed.heathanalytics.server.model.Report;
import org.snomed.heathanalytics.server.model.ReportDefinition;
import org.snomed.heathanalytics.server.model.StatisticalCorrelationReport;
import org.snomed.heathanalytics.server.model.StatisticalCorrelationReportDefinition;
import org.snomed.heathanalytics.server.pojo.Stats;
import org.snomed.heathanalytics.server.service.PatientQueryService;
import org.snomed.heathanalytics.server.service.ReportService;
import org.snomed.heathanalytics.server.service.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Api(tags = "Reports", description = "-")
public class ReportController {

	@Autowired
	private ReportService reportService;

	@Autowired
	private PatientQueryService patientQueryService;

	@ApiOperation(value = "Service statistics.", notes = "Just reports the server date and number of patients in the store.")
	@RequestMapping(value = "/stats", method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public Stats getStats() {
		return patientQueryService.getStats();
	}

	@ApiOperation(value = "Create a report of patient counts using groups.",
			notes = "Create a report of patient counts using groups.\n" +
					"\n" +
					"A top level criteria can be defined for the overall cohort.  \n" +
					"Groups with more specific criteria can also be defined if required.  \n" +
					"A count of matching patients will be returned for the top level and each specified group.  \n" +
					"The top level criteria will be included in each group automatically so there is no need to repeat the top level criteria.  \n" +
					"\n" +
					"For event criteria marked with includeCPTAnalysis=true a CPT report will be included in the response for that group.  \n" +
					"The CPT report will list the matching CPT codes for events which could be mapped to CPT together with total counts and total RVUs.  \n" +
					"This functionality requires CPT codes and SNOMED CT to CPT mapping to be loaded.  \n" +
					"\n" +
					"In the report request the groups section is a list of lists. If a second list of groups is given " +
					"these will become subgroups within each of the first list of groups.  \n" +
					"For example if the groups in the request are: `[Smoker, Non-Smoker], [Foot Amputation]`  \n" +
					"the groups in the results will be: `[Smoker, [(Smoker +) Foot Amputation]], [Non-Smoker, [(Non-Smoker +) Foot Amputation]]`.  \n" +
					"The criteria of top level groups is inherited by subgroups. There is no limit to the number of subgroup levels.  \n" +
					"\n" +
					"Within 'eventCriteria' either 'conceptECL' or 'conceptSubsetId' must be used, all other fields are optional.  \n" +
					"In the days fields a value of `-1` means unbounded. For example .")
	@RequestMapping(value = "/report", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Report runReport(@RequestBody ReportDefinition reportDefinition) throws ServiceException {
		return reportService.runReport(reportDefinition);
	}

	@ApiOperation(value = "Statistical event correlation report.",
			notes = "Within eventCriteria days value of '-1' can be used as an unbounded value.")
	@RequestMapping(value = "/statistical-correlation-report", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public StatisticalCorrelationReport runReportSta(@RequestBody StatisticalCorrelationReportDefinition reportDefinition) throws ServiceException {
		return reportService.runStatisticalReport(reportDefinition);
	}

}
