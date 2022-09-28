package org.snomed.heathanalytics.server.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

@Service
public class SnowstormProxyService {

	@Value("${snowstorm.url}")
	private String snowstormUrl;

	private final Logger logger = LogManager.getLogger(getClass());

	public void processProxyRequest(String body, HttpMethod method,
			HttpServletRequest request, String requestUrl, HttpServletResponse response) throws URISyntaxException {

		URI uri = new URI(snowstormUrl);

		// replacing context path form uri to match actual gateway URI
		String queryString = request.getQueryString();
		String url = UriComponentsBuilder.fromUri(uri)
				.path(requestUrl)
				.build(true).toUri() + (queryString != null ? ("?" + URLDecoder.decode(queryString, StandardCharsets.UTF_8)) : "");

		HttpHeaders headers = new HttpHeaders();
		Enumeration<String> headerNames = request.getHeaderNames();

		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			headers.set(headerName, request.getHeader(headerName));
		}

		headers.remove(HttpHeaders.ACCEPT_ENCODING);

		HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
		ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
		RestTemplate restTemplate = new RestTemplate(factory);
		try {
			ResponseEntity<String> serverResponse = restTemplate.exchange(url, method, httpEntity, String.class);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.put(HttpHeaders.CONTENT_TYPE, serverResponse.getHeaders().get(HttpHeaders.CONTENT_TYPE));
			response.setStatus(serverResponse.getStatusCodeValue());
			String responseBody = serverResponse.getBody();
			if (responseBody != null) {
				try {
					response.getWriter().write(responseBody);
				} catch (IOException e) {
					logger.warn("Failed to write proxy body to response.");
				}
			}
		} catch (HttpStatusCodeException e) {
			try {
				response.setStatus(e.getRawStatusCode());
				response.getWriter().write(e.getResponseBodyAsString());
			} catch (IOException ex) {
				logger.warn("Failed to write proxy error to response.");
			}
		}
	}

}
