package org.snomed.heathanalytics;

import org.snomed.heathanalytics.ingestion.exampledata.ExampleConceptService;
import org.snomed.heathanalytics.ingestion.exampledata.ExampleDataGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
		context.getBean(Application.class).run();
	}

	public void run() {
//		exampleDataSource().stream();
	}

	@Bean
	public ExampleDataGenerator exampleDataSource() {
		return new ExampleDataGenerator(new ExampleConceptService(), 100);
	}

}
