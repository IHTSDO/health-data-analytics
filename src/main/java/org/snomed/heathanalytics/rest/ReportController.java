package org.snomed.heathanalytics.rest;

import io.swagger.annotations.ApiOperation;
import org.snomed.heathanalytics.domain.Report;
import org.snomed.heathanalytics.domain.ReportDefinition;
import org.snomed.heathanalytics.domain.StatisticalCorrelationReport;
import org.snomed.heathanalytics.domain.StatisticalCorrelationReportDefinition;
import org.snomed.heathanalytics.service.QueryService;
import org.snomed.heathanalytics.service.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReportController {

	@Autowired
	private QueryService queryService;

	@ApiOperation(value = "Statistical encounter correlation report.",
			notes = "Within encounterCriteria days value of '-1' can be used as an unbounded value.")
	@RequestMapping(value = "/statistical-correlation-report", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public StatisticalCorrelationReport runReportSta(@RequestBody StatisticalCorrelationReportDefinition reportDefinition) throws ServiceException {
		return queryService.runStatisticalReport(reportDefinition);
	}

}
