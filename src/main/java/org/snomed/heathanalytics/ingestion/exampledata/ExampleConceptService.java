package org.snomed.heathanalytics.ingestion.exampledata;

public class ExampleConceptService {

	public String selectChildOf(String conceptId) {
		// TODO - select a fully defined child of this concept from a terminology server
		// For now we will just return the concept itself
		return conceptId;
	}
}
