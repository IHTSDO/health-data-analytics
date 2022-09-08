package org.snomed.heathanalytics.server.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ValueSet;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;
import static java.lang.String.format;

public class SnomedConceptService {

	private final IGenericClient fhirClient;
	private final Map<Long, List<Long>> conceptDescendantMap;

	public SnomedConceptService() {
		fhirClient = FhirContext.forR4().newRestfulGenericClient("https://snowstorm.ihtsdotools.org/fhir");
		conceptDescendantMap = new Long2ObjectOpenHashMap<>();
	}

	public Long expandValueSet(String conceptId) throws ServiceException {
		long conceptIdLong = parseLong(conceptId);
		List<Long> descendants = conceptDescendantMap.get(conceptIdLong);
		if (descendants == null) {
			try {
				// Grab first page of concept codes
				Parameters parameters = fhirClient.operation().onType("ValueSet").named("$expand")
						.withParameters(new Parameters().addParameter("url", format("http://snomed.info/sct?fhir_vs=ecl/<%s", conceptId)))
						.useHttpGet()
						.execute();
				List<Parameters.ParametersParameterComponent> parameter = parameters.getParameter();
				Parameters.ParametersParameterComponent param = parameter.iterator().next();
				ValueSet valueSet = (ValueSet) param.getResource();
				List<ValueSet.ValueSetExpansionContainsComponent> contains = valueSet.getExpansion().getContains();
				descendants = contains.stream().map(ValueSet.ValueSetExpansionContainsComponent::getCode).map(Long::parseLong).collect(Collectors.toList());
				conceptDescendantMap.put(conceptIdLong, descendants);
			} catch (BaseServerResponseException exception) {
				throw new ServiceException("Failed to expand ECL using FHIR server.", exception);
			}
		}
		if (descendants.isEmpty()) {
			// Concept is a leaf, return self
			return conceptIdLong;
		}
		return descendants.get(ThreadLocalRandom.current().nextInt(0, descendants.size()));
	}

}
