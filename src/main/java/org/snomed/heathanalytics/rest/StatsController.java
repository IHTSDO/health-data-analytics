package org.snomed.heathanalytics.rest;

import org.snomed.heathanalytics.pojo.Stats;
import org.snomed.heathanalytics.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatsController {

	@Autowired
	private QueryService queryService;

	@RequestMapping(value = "/stats", method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public Stats getStats() {
		return queryService.getStats();
	}

}
