package org.snomed.heathanalytics.server.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ValueSet;
import org.snomed.heathanalytics.server.model.SnomedConstants;
import org.snomed.heathanalytics.server.pojo.ConceptResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.lang.String.format;

@Service
public class SnomedService {

	private final IGenericClient fhirClient;
	private final RestTemplate snowstormLiteRestTemplate;
	private final Map<String, List<Long>> eclResultsCache;

	@Value("${fhir.codesystem.snomed.uri}")
	private String snomedCodeSystemUri;

	public SnomedService(@Value("${fhir-terminology-server-url}") String fhirTerminologyServerUrl,
			@Value("${snowstorm-lite-url}") String snowstormLiteUrl) {
		FhirContext context = FhirContext.forR4();
		context.getRestfulClientFactory().setSocketTimeout(30_000);
		fhirClient = context.newRestfulGenericClient(fhirTerminologyServerUrl);
		snowstormLiteRestTemplate = new RestTemplateBuilder().rootUri(snowstormLiteUrl).build();
		eclResultsCache = new HashMap<>();
	}

	public List<Long> getConceptIds(String ecl) throws ServiceException {
		List<Long> results = eclResultsCache.get(ecl);
		if (results != null) {
			return results;
		} else {
			synchronized (eclResultsCache) {
				try {
					// Grab all concept codes
					List<Long> newResults = new LongArrayList();
					valueSetExpand(
							ecl,
							0,
							10_000,// No standardised pagination mechanism, grab first 10K for now
							null,
							stream -> stream.map(ValueSet.ValueSetExpansionContainsComponent::getCode).map(Long::parseLong).forEach(newResults::add));
					eclResultsCache.put(ecl, newResults);
					return newResults;
				} catch (BaseServerResponseException exception) {
					throw new ServiceException("Failed to expand ECL using FHIR server.", exception);
				}
			}
		}
	}

	public List<ConceptResult> findConcepts(String eclQuery, String prefix, int offset, int limit) {
		List<ConceptResult> results = new ArrayList<>();
		valueSetExpand(eclQuery, offset, limit, prefix, stream -> stream.forEach(code -> results.add(new ConceptResult(code.getCode(), code.getDisplay()))));
		return results;

	}

	public ConceptResult findConcept(String conceptId) {
		List<ConceptResult> concepts = findConcepts(conceptId, null, 0, 1);
		return concepts.isEmpty() ? null : concepts.get(0);
	}

	private void valueSetExpand(String ecl, int offset, int limit, String filter, Consumer<Stream<ValueSet.ValueSetExpansionContainsComponent>> resultConsumer) {
		Parameters requestParameters = new Parameters()
				.addParameter("url", format("%s?fhir_vs=ecl/%s", snomedCodeSystemUri, ecl))
				.addParameter("offset", offset + "")
				.addParameter("count", limit + "");
		if (filter != null) {
			requestParameters.addParameter("filter", filter);
		}
		Parameters parameters = fhirClient.operation().onType(ValueSet.class).named("$expand")
				.withParameters(requestParameters)
				.useHttpGet()
				.execute();
		List<Parameters.ParametersParameterComponent> parameter = parameters.getParameter();
		Parameters.ParametersParameterComponent param = parameter.iterator().next();
		ValueSet valueSet = (ValueSet) param.getResource();
		List<ValueSet.ValueSetExpansionContainsComponent> contains = valueSet.getExpansion().getContains();
		resultConsumer.accept(contains.stream());
	}

	public List<NodeWithParents> loadPartialHierarchy(Set<Long> codes) {
		String version = null;
		if (!snomedCodeSystemUri.equals(SnomedConstants.SNOMED_URI)) {
			version = snomedCodeSystemUri;
		}
		List<String> codeStrings = codes.stream().map(Object::toString).toList();
		HierarchyRequest hierarchyRequest = new HierarchyRequest(SnomedConstants.SNOMED_URI, version, codeStrings);
		ParameterizedTypeReference<List<NodeWithParents>> responseType = new ParameterizedTypeReference<>() {};
		HttpEntity<HierarchyRequest> requestEntity = new HttpEntity<>(hierarchyRequest);
		ResponseEntity<List<NodeWithParents>> response = snowstormLiteRestTemplate.exchange("/partial-hierarchy", HttpMethod.POST, requestEntity, responseType);
		return response.getBody();
	}

	public static class HierarchyRequest {

		private String system;
		private String version;
		private List<String> codes;

		public HierarchyRequest(String system, String version, List<String> codes) {
			this.system = system;
			this.version = version;
			this.codes = codes;
		}

		public String getSystem() {
			return system;
		}

		public void setSystem(String system) {
			this.system = system;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		public List<String> getCodes() {
			return codes;
		}

		public void setCodes(List<String> codes) {
			this.codes = codes;
		}
	}

	public record NodeWithParents(String code, String[] parents) {}

}
