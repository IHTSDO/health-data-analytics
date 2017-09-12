package org.snomed.heathanalytics.rest;

import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.domain.Subset;
import org.snomed.heathanalytics.service.CohortCriteria;
import org.snomed.heathanalytics.service.QueryService;
import org.snomed.heathanalytics.service.ServiceException;
import org.snomed.heathanalytics.store.SubsetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.web.bind.annotation.*;

import static org.snomed.heathanalytics.rest.ControllerHelper.aggregatedPageWorkaround;

@RestController
public class SubsetController {

	@Autowired
	private SubsetRepository subsetRepository;

	@RequestMapping(value = "/subsets", method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public org.springframework.data.domain.Page<Subset> listSubsets(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "100") int size) throws ServiceException {
		return aggregatedPageWorkaround(subsetRepository.findAll(new PageRequest(page, size)));
	}

	@RequestMapping(value = "/subsets/{subsetId}", method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public Subset getSubset(@PathVariable String subsetId) throws ServiceException {
		return subsetRepository.findOne(subsetId);
	}

	@RequestMapping(value = "/subsets", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Subset saveSubset(@RequestBody Subset subset) throws ServiceException {
		return subsetRepository.save(subset);
	}

	@RequestMapping(value = "/subsets/{subsetId}", method = RequestMethod.DELETE, produces = "application/json")
	@ResponseBody
	public void deleteSubset(@PathVariable String subsetId) throws ServiceException {
		subsetRepository.delete(subsetId);
	}

}
