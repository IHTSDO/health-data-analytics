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
import org.springframework.context.annotation.Lazy;

import java.io.File;
import java.util.List;

@SpringBootApplication(exclude = {
		ElasticsearchDataAutoConfiguration.class,
		ElasticsearchRestClientAutoConfiguration.class,
})
public class ServerApplication extends Config implements ApplicationRunner {

	public static final String IMPORT_POPULATION_NATIVE = "import-population";
	public static final String IMPORT_DATASET = "data-set";
	public static final String IMPORT_POPULATION_FHIR = "import-population-fhir";
	public static final String IMPORT_POPULATION_FHIR_SINGLE_RESOURCES = "import-population-fhir-single";
	public static final String IMPORT_FHIR_VERSION = "import-fhir-version";

	@Autowired
	@Lazy
	private ElasticOutputStream elasticOutputStream;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) {
		SpringApplication.run(ServerApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments applicationArguments) {
		try {
			if (applicationArguments.containsOption(IMPORT_POPULATION_NATIVE)) {
				List<String> values = applicationArguments.getOptionValues(IMPORT_POPULATION_NATIVE);
				if (values.size() != 1) {
					throw new IllegalArgumentException("Option " + IMPORT_POPULATION_NATIVE + " requires one directory name after the equals character.");
				}
				File directory = new File(values.get(0));
				checkDirectoryExists(directory);
				importPopulationNativeFormat(directory, getDataSetLabel(applicationArguments));
				System.exit(0);
			}
			if (applicationArguments.containsOption(IMPORT_POPULATION_FHIR)) {
				List<String> values = applicationArguments.getOptionValues(IMPORT_POPULATION_FHIR);
				if (values.size() != 1) {
					throw new IllegalArgumentException("Option " + IMPORT_POPULATION_FHIR + " requires one directory name after the equals character.");
				}
				File directory = new File(values.get(0));
				checkDirectoryExists(directory);
				importPopulationFHIRFormat(directory, getDataSetLabel(applicationArguments));
				System.exit(0);
			}
			if (applicationArguments.containsOption(IMPORT_POPULATION_FHIR_SINGLE_RESOURCES)) {
				List<String> values = applicationArguments.getOptionValues(IMPORT_POPULATION_FHIR_SINGLE_RESOURCES);
				if (values.size() != 1) {
					throw new IllegalArgumentException("Option " + IMPORT_POPULATION_FHIR_SINGLE_RESOURCES + " requires at least a directory name after the equals character.");
				}
				List<String> fhirVersionOption = applicationArguments.getOptionValues(IMPORT_FHIR_VERSION);
				final String fhirVersion = (fhirVersionOption != null && fhirVersionOption.size() == 1) ? fhirVersionOption.get(0) : "R4";
				importPopulationSingleFhirResources(new File(values.get(0)), fhirVersion, getDataSetLabel(applicationArguments));
				System.exit(0);
			}
		} catch (IllegalArgumentException e) {
			logger.error("Illegal Argument: {}", e.getMessage());
			System.exit(1);
		}
	}

	private String getDataSetLabel(ApplicationArguments applicationArguments) {
		List<String> values = applicationArguments.getOptionValues(IMPORT_DATASET);
		if (values == null || values.isEmpty()) {
			throw new IllegalArgumentException("Option " + IMPORT_DATASET + " is required. Please provide a label for this set of data.");
		}
		String dataset = values.get(0);
		if (!dataset.matches("[a-zA-Z0-9-]+")) {
			throw new IllegalArgumentException("Option " + IMPORT_DATASET + " can only use characters a-z, A-Z, 0-9 and hyphen.");
		}
		return dataset;
	}

	private void checkDirectoryExists(File directory) {
		if (!directory.isDirectory()) {
			logger.error("Directory not found: " + directory.getAbsolutePath());
			System.exit(1);
		}
	}

	private void importPopulationNativeFormat(File populationNDJSONDirectory, String dataSetLabel) {
		logger.info("******** Importing patient data in native format from {} ...", populationNDJSONDirectory.getPath());
		new LocalFileNDJsonIngestionSource(objectMapper()).stream(new LocalFileNDJsonIngestionSourceConfiguration(dataSetLabel, populationNDJSONDirectory), elasticOutputStream);
	}

	private void importPopulationSingleFhirResources(File populationSingleFhirResourcesDirectory, String fhirVersion, String dataSetLabel) {
		logger.info("******** Importing patient data from single FHIR {} resources from {} ...", fhirVersion, populationSingleFhirResourcesDirectory.getPath());
		new FHIRLocalIngestionSource().stream(new FHIRLocalIngestionSourceConfiguration(dataSetLabel, populationSingleFhirResourcesDirectory, fhirVersion), elasticOutputStream);
	}

	private void importPopulationFHIRFormat(File populationNDJSONDirectory, String dataSetLabel) {
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
		File serviceRequestFile = null;
		for (File file : files) {
			if (file.getName().startsWith("Patient")) {
				patientFile = file;
			} else if (file.getName().startsWith("Condition")) {
				conditionFile = file;
			} else if (file.getName().startsWith("Procedure")) {
				procedureFile = file;
			} else if (file.getName().startsWith("MedicationRequest")) {
				medicationRequestFile = file;
			} else if (file.getName().startsWith("ServiceRequest")) {
				serviceRequestFile = file;
			}
		}

		new FHIRBulkLocalIngestionSource(objectMapper()).stream(
				new FHIRBulkLocalIngestionSourceConfiguration(dataSetLabel, patientFile, conditionFile, procedureFile, medicationRequestFile, serviceRequestFile),
				elasticOutputStream);
	}

}
