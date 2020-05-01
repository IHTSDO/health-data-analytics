package org.snomed.heathanalytics.datageneration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

@SpringBootApplication(exclude = {ElasticsearchAutoConfiguration.class, ElasticsearchDataAutoConfiguration.class})
public class Application implements ApplicationRunner {

	public static final String POPULATION_SIZE = "population-size";
	public static final String OUTPUT_DIR = "patient-data-for-import";
	public static final int POPULATION_SIZE_DEFAULT = 1_248_322;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(ApplicationArguments applicationArguments) throws Exception {
		int populationSize = POPULATION_SIZE_DEFAULT;
		if (applicationArguments.containsOption(POPULATION_SIZE)) {
			List<String> values = applicationArguments.getOptionValues(POPULATION_SIZE);
			if (values == null || values.size() != 1 || !values.get(0).matches("\\d*")) {
				throw new IllegalArgumentException("Option " + POPULATION_SIZE + " requires one numeric value after the equals character.");
			}
			populationSize = Integer.parseInt(values.get(0));
		}
		generatePopulation(populationSize);
		System.exit(0);
	}

	@Bean
	public ExampleDataGenerator exampleDataSource() {
		File releaseDirectory = new File("release");
		return new ExampleDataGenerator(new ExampleConceptService(releaseDirectory));
	}

	@Bean
	public ObjectMapper objectMapper() {
		return Jackson2ObjectMapperBuilder
				.json()
				.serializationInclusion(JsonInclude.Include.NON_NULL)
				.build();
	}

	private void generatePopulation(int demoPatientCount) throws IOException {
		logger.info("******** Generating data for {} patients ...", new DecimalFormat( "#,###,###" ).format(demoPatientCount));
		File dataGenDir = new File(OUTPUT_DIR);
		dataGenDir.mkdirs();
		File patientsNdJsonFile = new File(dataGenDir, "generated-patients.ndjson");
		exampleDataSource().createPatients(demoPatientCount, patientsNdJsonFile);
	}

}
