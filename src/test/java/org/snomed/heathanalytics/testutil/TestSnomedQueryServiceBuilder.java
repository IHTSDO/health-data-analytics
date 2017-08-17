package org.snomed.heathanalytics.testutil;

import org.ihtsdo.otf.snomedboot.factory.implementation.standard.ConceptImpl;
import org.ihtsdo.otf.sqs.service.ReleaseWriter;
import org.ihtsdo.otf.sqs.service.SnomedQueryService;
import org.ihtsdo.otf.sqs.service.store.RamReleaseStore;

import java.io.IOException;
import java.text.ParseException;

public class TestSnomedQueryServiceBuilder {

	public static SnomedQueryService createBlank() throws IOException, ParseException {
		RamReleaseStore releaseStore = new RamReleaseStore();
		ReleaseWriter releaseWriter = new ReleaseWriter(releaseStore);
		ConceptImpl concept = new ConceptImpl("1", "20170731", true, "", "");
		concept.setFsn("");
		releaseWriter.addConcept(concept, false);
		releaseWriter.close();
		return new SnomedQueryService(releaseStore);
	}

	public static SnomedQueryService createWithConcepts(ConceptImpl... concepts) throws IOException, ParseException {
		RamReleaseStore luceneConceptStore = new RamReleaseStore();
		ReleaseWriter conceptStoreWriter = new ReleaseWriter(luceneConceptStore);
		for (ConceptImpl concept : concepts) {
			conceptStoreWriter.addConcept(concept, false);
		}
		conceptStoreWriter.close();
		return new SnomedQueryService(luceneConceptStore);
	}

	public static ConceptImpl concept(String id, String... ancestors) {
		ConceptImpl concept = new ConceptImpl(id, "20170731", true, "", "");
		concept.setFsn("");
		for (String ancestor : ancestors) {
			concept.addInferredParent(concept(ancestor));
		}
		return concept;
	}
}
