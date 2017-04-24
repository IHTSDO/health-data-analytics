package org.snomed.heathanalytics;

import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.factory.LoadingProfile;
import org.ihtsdo.otf.sqs.service.ReleaseImportManager;
import org.ihtsdo.otf.sqs.service.SnomedQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.ingestion.elasticsearch.ElasticOutputStream;
import org.snomed.heathanalytics.ingestion.exampledata.ExampleConceptService;
import org.snomed.heathanalytics.ingestion.exampledata.ExampleDataGenerator;
import org.snomed.heathanalytics.service.QueryService;
import org.snomed.heathanalytics.service.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

@SpringBootApplication
public class Application implements ApplicationRunner {

	@Autowired
	private ElasticOutputStream elasticOutputStream;

	@Autowired
	private QueryService queryService;

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	private int demoPatientCount = 10000;
	private Logger logger = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(ApplicationArguments applicationArguments) throws Exception {
		// if (applicationArguments.containsOption("demo-mode")) {
		// Force Demo Mode
			runDemo();
		// }
	}

	@Bean
	public SnomedQueryService snomedQueryService() throws IOException, ReleaseImportException {
		ReleaseImportManager importManager = new ReleaseImportManager();
		if (!importManager.isReleaseStoreExists()) {
			logger.info("SRS Index does not yet exist. Importing release to build disk based index.");
			importManager.loadReleaseFiles(new File("release"),
					LoadingProfile.light.withoutInactiveConcepts().withoutAnyRefsets());
		}
		return new SnomedQueryService(importManager.openExistingReleaseStore());
	}

	@Bean
	public ExampleDataGenerator exampleDataSource() throws IOException, ReleaseImportException {
		return new ExampleDataGenerator(new ExampleConceptService(snomedQueryService()), demoPatientCount);
	}

	private void runDemo() throws ServiceException, IOException, ReleaseImportException {
		System.out.println();
		deleteDemoData();

		logger.info("******** DEMO MODE ********");

		logger.info("******** Generating data for {} patients ...", new DecimalFormat( "#,###,###" ).format(demoPatientCount));
		exampleDataSource().stream(elasticOutputStream);

		Page<Patient> cohort = queryService.fetchCohort("<<420868002");// Disorder due to type 1 diabetes mellitus
		logger.info("******** Fetched 'Diabetes type 1' cohort, size:{}", cohort.getTotalElements());
		System.out.println("First 100 results:");
		for (Patient patient : cohort) {
			System.out.println(patient);
		}

		System.out.println("Demo complete.");
		System.out.println();
	}

	private void deleteDemoData() {
		elasticsearchTemplate.deleteIndex(ClinicalEncounter.class);
		elasticsearchTemplate.deleteIndex(Patient.class);
	}
}
