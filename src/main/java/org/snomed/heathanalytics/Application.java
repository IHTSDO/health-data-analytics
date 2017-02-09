package org.snomed.heathanalytics;

import org.snomed.heathanalytics.ingestion.exampledata.ExampleConceptService;
import org.snomed.heathanalytics.ingestion.exampledata.ExampleDataGenerator;
import org.snomed.heathanalytics.ingestion.store.elasticsearch.ElasticOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

	@Autowired
	private ElasticOutputStream elasticOutputStream;

	public void run() {
		exampleDataSource().stream(elasticOutputStream);
	}

	@Bean
	public ExampleDataGenerator exampleDataSource() {
		return new ExampleDataGenerator(new ExampleConceptService(), 100);
	}

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
		context.getBean(Application.class).run();
	}

}
