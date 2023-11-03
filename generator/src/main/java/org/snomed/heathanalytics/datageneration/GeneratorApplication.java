package org.snomed.heathanalytics.datageneration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;

@SpringBootApplication(exclude = {ElasticsearchRepositoriesAutoConfiguration.class, ElasticsearchDataAutoConfiguration.class})
public class GeneratorApplication implements ApplicationRunner {

	public static final String POPULATION_SIZE = "population-size";
	public static final String LONGITUDINAL = "longitudinal";
	public static final String OUTPUT_DIR = "patient-data-for-import";
	public static final String LONGITUDINAL_OUTPUT_DIR = "patient-data-for-import-longitudinal";
	public static final int POPULATION_SIZE_DEFAULT = 1_248_322;

	public static void main(String[] args) {
		SpringApplication.run(GeneratorApplication.class, args);
	}

	@Autowired
	private SnomedConceptService snomedConceptService;

	@Override
	public void run(ApplicationArguments applicationArguments) throws Exception {
		testTerminologyServerConnection();
		if (applicationArguments.containsOption(LONGITUDINAL)) {
			generateLongitudinalPopulation();
		} else {
			int populationSize = POPULATION_SIZE_DEFAULT;
			if (applicationArguments.containsOption(POPULATION_SIZE)) {
				List<String> values = applicationArguments.getOptionValues(POPULATION_SIZE);
				if (values == null || values.size() != 1 || !values.get(0).matches("\\d*")) {
					throw new IllegalArgumentException("Option " + POPULATION_SIZE + " requires one numeric value after the equals character.");
				}
				populationSize = Integer.parseInt(values.get(0));
			}
			generatePopulation(populationSize);
		}
		System.exit(0);
	}

	private void testTerminologyServerConnection() throws ServiceException {
		snomedConceptService.selectRandomDescendantOf("195967001");
	}

	@Bean
	public DemoPatientDataGenerator exampleDataSource() {
		return new DemoPatientDataGenerator(snomedConceptService);
	}

	@Bean
	public ObjectMapper objectMapper() {
		return Jackson2ObjectMapperBuilder
				.json()
				.serializationInclusion(JsonInclude.Include.NON_NULL)
				.build();
	}

	private void generatePopulation(int demoPatientCount) throws IOException {
		File dataGenDir = new File(OUTPUT_DIR);
		dataGenDir.mkdirs();
		File patientsNdJsonFile = new File(dataGenDir, "generated-patients.ndjson");
		if (patientsNdJsonFile.isFile()) {
			patientsNdJsonFile.delete();
		}
		exampleDataSource().createPatients(demoPatientCount, patientsNdJsonFile);
	}

	private void generateLongitudinalPopulation() throws IOException, ServiceException {
		File dataGenDir = new File(LONGITUDINAL_OUTPUT_DIR);
		dataGenDir.mkdirs();
		File longitudinalNdJsonFile = new File(dataGenDir, "generated-patients-longitudinal.ndjson");
		if (longitudinalNdJsonFile.isFile()) {
			longitudinalNdJsonFile.delete();
		}
		exampleDataSource().createLongitudinalPatients(longitudinalNdJsonFile);
	}

}
