package org.snomed.heathanalytics.server.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ValueSet;
import org.snomed.heathanalytics.server.pojo.ConceptResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.lang.String.format;

@Service
public class SnomedService {

	private final IGenericClient fhirClient;

	private final Map<String, List<Long>> eclResultsCache;

	public SnomedService(@Value("${fhir-terminology-server-url}") String fhirTerminologyServerUrl) {
		FhirContext context = FhirContext.forR4();
		context.getRestfulClientFactory().setSocketTimeout(30_000);
		fhirClient = context.newRestfulGenericClient(fhirTerminologyServerUrl);
		eclResultsCache = new HashMap<>();
	}

	public List<Long> getConceptIds(String ecl) throws ServiceException {
		System.out.println("_getConceptIds " + ecl);

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
		ecl = URLEncoder.encode(ecl, StandardCharsets.UTF_8);
		Parameters requestParameters = new Parameters()
				.addParameter("url", format("http://snomed.info/sct?fhir_vs=ecl/%s", ecl))
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
}
