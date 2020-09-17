package org.snomed.heathanalytics.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.factory.LoadingProfile;
import org.ihtsdo.otf.sqs.service.ReleaseImportManager;
import org.ihtsdo.otf.sqs.service.SnomedQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.server.ingestion.elasticsearch.ElasticOutputStream;
import org.snomed.heathanalytics.server.ingestion.fhir.FHIRBulkLocalIngestionSource;
import org.snomed.heathanalytics.server.ingestion.fhir.FHIRBulkLocalIngestionSourceConfiguration;
import org.snomed.heathanalytics.server.ingestion.localdisk.LocalFileNDJsonIngestionSource;
import org.snomed.heathanalytics.server.ingestion.localdisk.LocalFileNDJsonIngestionSourceConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Predicates.not;
import static springfox.documentation.builders.PathSelectors.regex;

@SpringBootApplication(exclude = ElasticsearchAutoConfiguration.class)
public class Application implements ApplicationRunner {

	public static final String IMPORT_POPULATION_NATIVE = "import-population";
	public static final String IMPORT_POPULATION_FHIR = "import-population-fhir";

	private static final File INDEX_DIRECTORY = new File("snomed-index");

	@Autowired
	private ElasticOutputStream elasticOutputStream;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(ApplicationArguments applicationArguments) throws Exception {
		if (applicationArguments.containsOption(IMPORT_POPULATION_NATIVE)) {
			List<String> values = applicationArguments.getOptionValues(IMPORT_POPULATION_NATIVE);
			if (values.size() != 1) {
				throw new IllegalArgumentException("Option " + IMPORT_POPULATION_NATIVE + " requires one directory name after the equals character.");
			}
			importPopulationNativeFormat(new File(values.get(0)));
			System.exit(0);
		}
		if (applicationArguments.containsOption(IMPORT_POPULATION_FHIR)) {
			List<String> values = applicationArguments.getOptionValues(IMPORT_POPULATION_FHIR);
			if (values.size() != 1) {
				throw new IllegalArgumentException("Option " + IMPORT_POPULATION_FHIR + " requires one directory name after the equals character.");
			}
			importPopulationFHIRFormat(new File(values.get(0)));
			System.exit(0);
		}
	}

	@Bean
	public ElasticsearchRestTemplate elasticsearchTemplate(@Value("${spring.data.elasticsearch.cluster-nodes}") String nodes) {
		List<HttpHost> httpHosts = new ArrayList<>();
		try {
			String[] split = nodes.split(",");
			for (String node : split) {
				node = node.trim();
				String[] split1 = node.split(":");
				String hostname = split1[0];
				int port = Integer.parseInt(split1[1]);
				httpHosts.add(new HttpHost(hostname, port));
			}
		} catch (Exception e) {
			logger.error("Failed to parse Elasticsearch cluster-nodes configuration value '{}'", nodes);
			throw e;
		}
		return new ElasticsearchRestTemplate(new RestHighLevelClient(RestClient.builder(httpHosts.toArray(new HttpHost[]{}))));
	}

	@Bean
	public SnomedQueryService snomedQueryService() throws IOException, ReleaseImportException {
		ReleaseImportManager importManager = new ReleaseImportManager();
		if (!importManager.isReleaseStoreExists(INDEX_DIRECTORY)) {
			logger.info("SRS Index does not yet exist. Importing release to build disk based index.");
			importManager.loadReleaseFilesToDiskBasedIndex(
					new File("release"),
					LoadingProfile.light.withoutInactiveConcepts().withoutAnyRefsets(),
					INDEX_DIRECTORY);
		}
		return new SnomedQueryService(importManager.openExistingReleaseStore(INDEX_DIRECTORY));
	}

	@Bean
	public ObjectMapper objectMapper() {
		return Jackson2ObjectMapperBuilder
				.json()
				.serializationInclusion(JsonInclude.Include.NON_NULL)
				.build();
	}

	private void importPopulationNativeFormat(File populationNDJSONDirectory) {
		logger.info("******** Importing patent data in native format from {} ...", populationNDJSONDirectory.getPath());
		new LocalFileNDJsonIngestionSource(objectMapper()).stream(new LocalFileNDJsonIngestionSourceConfiguration(populationNDJSONDirectory), elasticOutputStream);
	}

	private void importPopulationFHIRFormat(File populationNDJSONDirectory) {
		logger.info("******** Importing patent data in FHIR format from {} ...", populationNDJSONDirectory.getPath());
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
