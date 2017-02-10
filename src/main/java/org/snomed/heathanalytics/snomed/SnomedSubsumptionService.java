package org.snomed.heathanalytics.snomed;

import org.ihtsdo.otf.snomedboot.ComponentStore;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.ReleaseImporter;
import org.ihtsdo.otf.snomedboot.factory.LoadingProfile;
import org.ihtsdo.otf.snomedboot.factory.implementation.standard.ComponentFactoryImpl;
import org.ihtsdo.otf.snomedboot.factory.implementation.standard.ConceptImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.store.ConceptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

@Service
public class SnomedSubsumptionService {

	@Autowired
	private ConceptRepository conceptRepository;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public void loadSnomedRelease(InputStream snomedReleaseZipStreamIn) throws IOException, ReleaseImportException {
		// Use 'Snomed Boot' project to unzip release and build transitive closure
		ComponentStore componentStore = new ComponentStore();
		new ReleaseImporter().loadSnapshotReleaseFiles(snomedReleaseZipStreamIn, LoadingProfile.light.withoutInactiveConcepts(), new ComponentFactoryImpl(componentStore));

		logger.info("Storing {} Snomed concepts including transitive closure...", componentStore.getConcepts().size());
		Set<Concept> batch = new HashSet<>();
		int i = 1;
		for (ConceptImpl concept : componentStore.getConcepts().values()) {
			batch.add(new Concept(concept.getId(), concept.getFsn(), concept.getDefinitionStatusId(), concept.getAncestorIds()));
			if (i % 10000 == 0) {
				System.out.print(".");
				conceptRepository.save(batch);
				batch.clear();
			}
			if (i % 100000 == 0) {
				System.out.println();
			}
			i++;
		}
		if (!batch.isEmpty()) {
			conceptRepository.save(batch);
		}
		System.out.println();
	}

}
