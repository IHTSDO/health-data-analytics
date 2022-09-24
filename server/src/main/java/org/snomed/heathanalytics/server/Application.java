package org.snomed.heathanalytics.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.server.config.Config;
import org.snomed.heathanalytics.server.ingestion.elasticsearch.ElasticOutputStream;
import org.snomed.heathanalytics.server.ingestion.fhir.FHIRBulkLocalIngestionSource;
import org.snomed.heathanalytics.server.ingestion.fhir.FHIRBulkLocalIngestionSourceConfiguration;
import org.snomed.heathanalytics.server.ingestion.fhir.FHIRLocalIngestionSource;
import org.snomed.heathanalytics.server.ingestion.fhir.FHIRLocalIngestionSourceConfiguration;
import org.snomed.heathanalytics.server.ingestion.localdisk.LocalFileNDJsonIngestionSource;
import org.snomed.heathanalytics.server.ingestion.localdisk.LocalFileNDJsonIngestionSourceConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import static com.google.common.base.Predicates.not;
import static springfox.documentation.builders.PathSelectors.regex;

@SpringBootApplication(exclude = {
		ElasticsearchDataAutoConfiguration.class,
		ElasticsearchRestClientAutoConfiguration.class,
})
public class Application extends Config implements ApplicationRunner {

	public static final String IMPORT_POPULATION_NATIVE = "import-population";
	public static final String IMPORT_DATASET = "import-dataset";
	public static final String IMPORT_POPULATION_FHIR = "import-population-fhir";
	public static final String IMPORT_POPULATION_FHIR_SINGLE_RESOURCES = "import-population-fhir-single";
	public static final String IMPORT_FHIR_VERSION = "import-fhir-version";

	@Autowired
	private ElasticOutputStream elasticOutputStream;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(ApplicationArguments applicationArguments) {
		if (applicationArguments.containsOption(IMPORT_POPULATION_NATIVE)) {
			List<String> values = applicationArguments.getOptionValues(IMPORT_POPULATION_NATIVE);
			if (values.size() != 1) {
				throw new IllegalArgumentException("Option " + IMPORT_POPULATION_NATIVE + " requires one directory name after the equals character.");
			}
			File directory = new File(values.get(0));
			checkDirectoryExists(directory);
			importPopulationNativeFormat(directory);
			System.exit(0);
		}
		if (applicationArguments.containsOption(IMPORT_POPULATION_FHIR)) {
			List<String> values = applicationArguments.getOptionValues(IMPORT_POPULATION_FHIR);
			if (values.size() != 1) {
				throw new IllegalArgumentException("Option " + IMPORT_POPULATION_FHIR + " requires one directory name after the equals character.");
			}
			File directory = new File(values.get(0));
			checkDirectoryExists(directory);
			importPopulationFHIRFormat(directory);
			System.exit(0);
		}
		if (applicationArguments.containsOption(IMPORT_POPULATION_FHIR_SINGLE_RESOURCES)) {
			List<String> values = applicationArguments.getOptionValues(IMPORT_POPULATION_FHIR_SINGLE_RESOURCES);
			if (values.size() != 1) {
				throw new IllegalArgumentException("Option " + IMPORT_POPULATION_FHIR_SINGLE_RESOURCES + " requires at least a directory name after the equals character.");
			}
			List<String> fhirVersionOption = applicationArguments.getOptionValues(IMPORT_FHIR_VERSION);
			final String fhirVersion = (fhirVersionOption != null && fhirVersionOption.size()==1)?fhirVersionOption.get(0):"R4";
			importPopulationSingleFhirResources(new File(values.get(0)), fhirVersion);
			System.exit(0);
		}
	}

	private void checkDirectoryExists(File directory) {
		if (!directory.isDirectory()) {
			logger.error("Directory not found: " + directory.getAbsolutePath());
			System.exit(1);
		}
	}

	private void importPopulationNativeFormat(File populationNDJSONDirectory) {
		logger.info("******** Importing patient data in native format from {} ...", populationNDJSONDirectory.getPath());
		new LocalFileNDJsonIngestionSource(objectMapper()).stream(new LocalFileNDJsonIngestionSourceConfiguration(populationNDJSONDirectory), elasticOutputStream);
	}

	private void importPopulationSingleFhirResources(File populationSingleFhirResourcesDirectory, String fhirVersion) {
		logger.info("******** Importing patient data from single FHIR {} resources from {} ...", fhirVersion, populationSingleFhirResourcesDirectory.getPath());
		new FHIRLocalIngestionSource().stream(new FHIRLocalIngestionSourceConfiguration(populationSingleFhirResourcesDirectory, fhirVersion), elasticOutputStream);
	}

	private void importPopulationFHIRFormat(File populationNDJSONDirectory) {
		logger.info("******** Importing patient data in FHIR format from {} ...", populationNDJSONDirectory.getPath());
		if (!populationNDJSONDirectory.isDirectory()) {
			throw new IllegalArgumentException(String.format("The path '%s' is not a directory.", populationNDJSONDirectory.getAbsolutePath()));
		}

		File[] files = populationNDJSONDirectory.listFiles((dir, name) -> name.endsWith(".ndjson"));
		if (files == null || files.length == 0) {
			throw new IllegalArgumentException(String.format("No files with '.ndjson' extension found in directory '%s'.", populationNDJSONDirectory.getAbsolutePath()));
		}

		File patientFile = null;
		File conditionFile = null;
		File procedureFile = null;
		File medicationRequestFile = null;
		for (File file : files) {
			if (file.getName().startsWith("Patient")) {
				patientFile = file;
			} else if (file.getName().startsWith("Condition")) {
				conditionFile = file;
			} else if (file.getName().startsWith("Procedure")) {
				procedureFile = file;
			} else if (file.getName().startsWith("MedicationRequest")) {
				medicationRequestFile = file;
			}
		}

		new FHIRBulkLocalIngestionSource(objectMapper()).stream(
				new FHIRBulkLocalIngestionSourceConfiguration(patientFile, conditionFile, procedureFile, medicationRequestFile),
				elasticOutputStream);
	}

	@Bean
	public Docket api() {
		return new Docket(DocumentationType.SWAGGER_2)
				.select()
				.apis(RequestHandlerSelectors.any())
				.paths(not(regex("/error")))
				.build();
	}
}
