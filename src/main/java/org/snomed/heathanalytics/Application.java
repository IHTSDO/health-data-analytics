package org.snomed.heathanalytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.ingestion.elasticsearch.ElasticOutputStream;
import org.snomed.heathanalytics.ingestion.exampledata.ExampleConceptService;
import org.snomed.heathanalytics.ingestion.exampledata.ExampleDataGenerator;
import org.snomed.heathanalytics.query.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;

import java.text.DecimalFormat;

@SpringBootApplication
public class Application {

	@Autowired
	private ElasticOutputStream elasticOutputStream;

	@Autowired
	private QueryService queryService;

	private boolean demoMode = true;
	private int demoPatientCount = 10 * 1000;
	private Logger logger = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
		context.getBean(Application.class).run();
	}

	public void run() {
		if (demoMode) {
			runDemo();
		}
	}

	@Bean
	public ExampleDataGenerator exampleDataSource() {
		return new ExampleDataGenerator(new ExampleConceptService(), demoPatientCount);
	}

	private void runDemo() {
		System.out.println();

		logger.info("******** DEMO MODE ********");

		logger.info("******** Generating data for {} patients ...", new DecimalFormat( "#,###,###" ).format(demoPatientCount));
		exampleDataSource().stream(elasticOutputStream);

		Page<ClinicalEncounter> cohort = queryService.fetchCohort("420868002");// Disorder due to type 1 diabetes mellitus
		logger.info("******** Fetched 'Diabetes type 1' cohort, size:{}", cohort.getTotalElements());

		System.out.println();
	}

}
