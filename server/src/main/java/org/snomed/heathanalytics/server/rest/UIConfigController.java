package org.snomed.heathanalytics.server.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.snomed.heathanalytics.server.config.UIProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "UI Config", description = "-")
public class UIConfigController {

	private final UIProperties uiProperties;

	public UIConfigController(UIProperties uiProperties) {
		this.uiProperties = uiProperties;
	}

	@Operation(summary = "UI feature configuration.", description = "Returns feature flags that control UI behaviour.")
	@GetMapping(value = "/ui-config", produces = "application/json")
	public UIConfig getUIConfig() {
		return new UIConfig(uiProperties.getFeatures().getCorrelationDiscovery().isEnabled());
	}

	public record UIConfig(boolean correlationDiscoveryEnabled) {
	}

}
