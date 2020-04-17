package org.snomed.heathanalytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.factory.LoadingProfile;
import org.ihtsdo.otf.sqs.service.ReleaseImportManager;
import org.ihtsdo.otf.sqs.service.SnomedQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.CohortCriteria;
import org.snomed.heathanalytics.domain.EncounterCriterion;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.ingestion.elasticsearch.ElasticOutputStream;
import org.snomed.heathanalytics.ingestion.exampledata.ExampleConceptService;
import org.snomed.heathanalytics.ingestion.exampledata.ExampleDataGenerator;
import org.snomed.heathanalytics.ingestion.localdisk.LocalFileNDJsonIngestionSource;
import org.snomed.heathanalytics.ingestion.localdisk.LocalFileNDJsonIngestionSourceConfiguration;
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
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import static com.google.common.base.Predicates.not;
import static springfox.documentation.builders.PathSelectors.regex;

@SpringBootApplication
public class Application implements ApplicationRunner {

	public static final String GENERATE_POPULATION = "generate-population";
	public static final String DEMO_MODE = "demo-mode";

	private static final File INDEX_DIRECTORY = new File("index");

	@Autowired
	private ElasticOutputStream elasticOutputStream;

	@Autowired
	private QueryService queryService;

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	private int demoPatientCount = 1000 * 10;
	private Logger logger = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(ApplicationArguments applicationArguments) throws Exception {
		if (applicationArguments.containsOption(DEMO_MODE)) {
			runDemo(demoPatientCount);
		} else if (applicationArguments.containsOption(GENERATE_POPULATION)) {
			List<String> values = applicationArguments.getOptionValues(GENERATE_POPULATION);
			if (values.size() != 1 || !values.get(0).matches("\\d*")) {
				throw new IllegalArgumentException("Option " + GENERATE_POPULATION + " requires one numeric value after the equals character.");
			}
			generatePopulation(Integer.parseInt(values.get(0)));
		}
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
		return new SnomedQueryService(importManager.openExistingReleaseStore());
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

	private void runDemo(int demoPatientCount) throws ServiceException, IOException, ReleaseImportException {
		System.out.println();
		logger.info("******** DEMO MODE ********");

		generatePopulation(demoPatientCount);

		Page<Patient> cohort = queryService.fetchCohort(new CohortCriteria(new EncounterCriterion("<<420868002")), 0, 100);// Disorder due to type 1 diabetes mellitus
		logger.info("******** Fetched 'Diabetes type 1' cohort, size:{}", cohort.getTotalElements());
		System.out.println("First 100 results:");
		for (Patient patient : cohort) {
			System.out.println(patient);
		}

		System.out.println("Demo complete.");
		System.out.println();
	}

	private void generatePopulation(int demoPatientCount) throws IOException {
		deleteExistingPatientData();
		logger.info("******** Generating data for {} patients ...", new DecimalFormat( "#,###,###" ).format(demoPatientCount));
		File dataGenDir = new File("generated-patient-data");
		dataGenDir.mkdirs();
		File patientsNdJsonFile = new File(dataGenDir, "patients.ndjson");
		exampleDataSource().createPatients(demoPatientCount, patientsNdJsonFile);
		new LocalFileNDJsonIngestionSource(objectMapper()).stream(new LocalFileNDJsonIngestionSourceConfiguration(dataGenDir), elasticOutputStream);
	}

	private void deleteExistingPatientData() {
		DeleteQuery deleteQuery = new DeleteQuery();
		deleteQuery.setQuery(new MatchAllQueryBuilder());
		elasticsearchTemplate.delete(deleteQuery, Patient.class);
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
