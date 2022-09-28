package org.snomed.heathanalytics.server.rest;

import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.server.model.Subset;
import org.snomed.heathanalytics.server.service.InputValidationHelper;
import org.snomed.heathanalytics.server.store.SubsetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Api(tags = "Concept Subsets", description = "-")
public class SubsetController {

	@Autowired
	private SubsetRepository subsetRepository;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@RequestMapping(value = "/subsets", method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public org.springframework.data.domain.Page<Subset> listSubsets(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "100") int size,
			@RequestParam(required = false) String prefix) {

		PageRequest pageable = PageRequest.of(page, size);
		if (prefix == null || prefix.trim().isEmpty()) {
			return subsetRepository.findAll(pageable);
		} else {
			if (page != 0) {
				logger.warn("Searching for subsets with prefix param is only supported on page 0");
			}
			String[] parts = prefix.toLowerCase().split(" ");
			List<Subset> matchingSubsets = subsetRepository.findAll(PageRequest.of(page, 1_000))
					.getContent().stream()
					.filter(subset -> {
						String[] nameParts = subset.getName().toLowerCase().split(" ");
						for (String part : parts) {
							boolean partMatch = false;
							for (String namePart : nameParts) {
								if (namePart.startsWith(part)) {
									partMatch = true;
									break;
								}
							}
							if (!partMatch) {
								return false;
							}
						}
						return true;
					})
					.collect(Collectors.toList());
			return new PageImpl<>(matchingSubsets, pageable, matchingSubsets.size());
		}
	}

	@RequestMapping(value = "/subsets/{subsetId}", method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public Subset getSubset(@PathVariable String subsetId) {
		return subsetRepository.findById(subsetId).orElse(null);
	}

	@RequestMapping(value = "/subsets", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public Subset saveSubset(@RequestBody Subset subset) {
		InputValidationHelper.checkInput("Subset ID must be null or not empty.", subset.getId() == null || !subset.getId().isEmpty());
		return subsetRepository.save(subset);
	}

	@RequestMapping(value = "/subsets/{subsetId}", method = RequestMethod.PUT, produces = "application/json")
	@ResponseBody
	public Subset updateSubset(@PathVariable String subsetId, @RequestBody Subset subset) {
		InputValidationHelper.checkInput("Subset ID not be empty.",
				subsetId != null && !subsetId.isEmpty());
		InputValidationHelper.checkInput("Subset ID in path does not match the request body.",
				subsetId.equals(subset.getId()));

		return subsetRepository.save(subset);
	}

	@RequestMapping(value = "/subsets/{subsetId}", method = RequestMethod.DELETE, produces = "application/json")
	public void deleteSubset(@PathVariable String subsetId) {
		subsetRepository.deleteById(subsetId);
	}

}
