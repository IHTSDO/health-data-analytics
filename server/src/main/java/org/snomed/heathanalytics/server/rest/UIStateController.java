package org.snomed.heathanalytics.server.rest;

import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.server.service.UIStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@Api(tags = "UI State", description = "-")
@RequestMapping("/ui-state")
public class UIStateController {

	@Autowired
	private UIStateService service;
	private Logger logger = LoggerFactory.getLogger(getClass());

	@PostMapping("/{panel}/{id}")
	public void putUiState(@PathVariable String panel, @PathVariable String id, @RequestBody String jsonState) throws IOException {
		logger.info("Put '{}' '{}' {}", panel, id, jsonState);
		service.put(panel, id, jsonState);
	}

	@GetMapping("/{panel}")
	public List<String> listUiState(@PathVariable String panel) throws IOException {
		return service.list(panel);
	}

	@GetMapping("/{panel}/{id}")
	public String getUiState(@PathVariable String panel, @PathVariable String id) throws IOException {
		return service.get(panel, id);
	}

	@DeleteMapping("/{panel}/{id}")
	public void deleteUiState(@PathVariable String panel, @PathVariable String id) throws IOException {
		service.delete(panel, id);
	}

}
