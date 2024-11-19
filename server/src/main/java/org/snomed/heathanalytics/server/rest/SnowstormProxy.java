package org.snomed.heathanalytics.server.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.snomed.heathanalytics.server.service.SnowstormProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;

@RestController
@RequestMapping("/api/snowstorm")
public class SnowstormProxy {

	@Autowired
	private SnowstormProxyService service;

	@RequestMapping("/**")
	public void sendRequestToSPM(@RequestBody(required = false) String body,
			HttpMethod method, HttpServletRequest request, HttpServletResponse response)
			throws URISyntaxException {

		String requestURI = request.getRequestURI().replace("api/snowstorm/", "");
		service.processProxyRequest(body, method, request, requestURI, response);
	}

}
