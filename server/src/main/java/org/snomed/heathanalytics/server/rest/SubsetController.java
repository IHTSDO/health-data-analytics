package org.snomed.heathanalytics.server.rest;

import io.swagger.annotations.Api;
import org.snomed.heathanalytics.server.model.Subset;
import org.snomed.heathanalytics.server.pojo.EmptyPojo;
import org.snomed.heathanalytics.server.service.InputValidationHelper;
import org.snomed.heathanalytics.server.service.ServiceException;
import org.snomed.heathanalytics.server.store.SubsetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import static org.snomed.heathanalytics.server.rest.ControllerHelper.aggregatedPageWorkaround;

@RestController
@Api(tags = "Concept Subsets", description = "-")
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
		InputValidationHelper.checkInput("Subset ID must be null or not empty.", subset.getId() == null || !subset.getId().isEmpty());
		return subsetRepository.save(subset);
	}

	@RequestMapping(value = "/subsets/{subsetId}", method = RequestMethod.PUT, produces = "application/json")
	@ResponseBody
	public Subset updateSubset(@PathVariable String subsetId, @RequestBody Subset subset) throws ServiceException {
		InputValidationHelper.checkInput("Subset ID not be empty.",
				subsetId != null && !subsetId.isEmpty());
		InputValidationHelper.checkInput("Subset ID in path does not match the request body.",
				subsetId.equals(subset.getId()));

		return subsetRepository.save(subset);
	}

	@RequestMapping(value = "/subsets/{subsetId}", method = RequestMethod.DELETE, produces = "application/json")
	@ResponseBody
	public EmptyPojo deleteSubset(@PathVariable String subsetId) throws ServiceException {
		subsetRepository.delete(subsetId);
		return new EmptyPojo(); // This is a workaround for the frontend implementation.
	}

}
