package org.snomed.heathanalytics;

import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.ingestion.elasticsearch.ElasticOutputStream;
import org.snomed.heathanalytics.ingestion.exampledata.ExampleConceptService;
import org.snomed.heathanalytics.ingestion.exampledata.ExampleDataGenerator;
import org.snomed.heathanalytics.service.QueryService;
import org.snomed.heathanalytics.snomed.SnomedSubsumptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;

@SpringBootApplication
public class Application {

	@Autowired
	private ElasticOutputStream elasticOutputStream;

	@Autowired
	private QueryService queryService;

	@Autowired
	private SnomedSubsumptionService snomedSubsumptionService;

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	private boolean demoMode = true;
	private int demoPatientCount = 10 * 1000;
	private Logger logger = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) {
		if (new File("data").exists()) {
			throw new IllegalStateException("The 'data' directory already exists, please move or remove before running the demo.");
		}

		ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
		context.getBean(Application.class).run();
	}

	private void run() {
		if (demoMode) {
			runDemo();
		}
	}

	@Bean
	public ExampleDataGenerator exampleDataSource() {
		return new ExampleDataGenerator(new ExampleConceptService(snomedSubsumptionService), demoPatientCount);
	}

	private void runDemo() {
		System.out.println();
		deleteDemoData();

		logger.info("******** DEMO MODE ********");

		String demoSnomedReleaseZipPath = "release/SnomedCT_InternationalRF2_Production_20170131.zip";
		logger.info("******** Loading Snomed Release from {}", demoSnomedReleaseZipPath);
		try {
			snomedSubsumptionService.loadSnomedRelease(new FileInputStream(demoSnomedReleaseZipPath));
		} catch (IOException | ReleaseImportException e) {
			logger.error("Failed to load Snomed release zip", e);
			return;
		}

		logger.info("******** Generating data for {} patients ...", new DecimalFormat( "#,###,###" ).format(demoPatientCount));
		exampleDataSource().stream(elasticOutputStream);

		Page<ClinicalEncounter> cohort = queryService.fetchCohort("420868002");// Disorder due to type 1 diabetes mellitus
		logger.info("******** Fetched 'Diabetes type 1' cohort, size:{}", cohort.getTotalElements());
		System.out.println("First 100 results:");
		for (ClinicalEncounter clinicalEncounter : cohort) {
			System.out.println(clinicalEncounter);
		}

		System.out.println("Demo complete.");
		System.out.println();

		deleteDemoData();
	}

	private void deleteDemoData() {
		elasticsearchTemplate.deleteIndex(ClinicalEncounter.class);
		elasticsearchTemplate.deleteIndex(Patient.class);
	}

}
