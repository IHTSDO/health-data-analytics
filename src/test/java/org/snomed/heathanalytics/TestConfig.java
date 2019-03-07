package org.snomed.heathanalytics;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.ihtsdo.otf.sqs.service.SnomedQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.testutil.TestSnomedQueryServiceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.rest.ElasticsearchRestClient;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.embeddedelasticsearch.PopularProperties;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@PropertySource("application-test.properties")
@SpringBootApplication
public class TestConfig {

	private static final String ELASTIC_SEARCH_VERSION = "6.0.1";

	@Autowired
	private ElasticsearchOperations elasticsearchTemplate;

	private static EmbeddedElastic testElasticsearchSingleton;
	private static File installationDirectory;
	private static final int PORT = 9931;

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Bean
	public ElasticsearchTemplate elasticsearchTemplate() {
		return new ElasticsearchTemplate(elasticsearchClient());
	}

	@Bean
	public ElasticsearchRestClient elasticsearchClient() {
		// Share the Elasticsearch instance between test contexts
		if (testElasticsearchSingleton == null) {
			// Create and start a clean standalone Elasticsearch test instance
			String clusterName = "snowstorm-integration-test-cluster";
			try {
				installationDirectory = new File(System.getProperty("java.io.tmpdir"), "embedded-elasticsearch-temp-dir");
				testElasticsearchSingleton = EmbeddedElastic.builder()
						.withElasticVersion(ELASTIC_SEARCH_VERSION)
						.withStartTimeout(30, TimeUnit.SECONDS)
						.withSetting(PopularProperties.CLUSTER_NAME, clusterName)
						.withSetting(PopularProperties.HTTP_PORT, PORT)
						// Manually delete installation directory to prevent verbose error logging
						.withCleanInstallationDirectoryOnStop(false)
						.withInstallationDirectory(installationDirectory)
						.build();
				testElasticsearchSingleton
						.start()
						.deleteIndices();
			} catch (InterruptedException | IOException e) {
				throw new RuntimeException("Failed to start standalone Elasticsearch instance.", e);
			}
		}

		// Create client to to standalone instance
		return new ElasticsearchRestClient(new HashMap<>(), "http://localhost:" + PORT);
	}

	@Bean
	public SnomedQueryService snomedQueryService() throws IOException, ParseException {
		return TestSnomedQueryServiceBuilder.createBlank();
	}

	@PreDestroy
	public void shutdown() {
		synchronized (TestConfig.class) {
			if (testElasticsearchSingleton != null) {
				elasticsearchTemplate.deleteIndex(Patient.class);
				try {
					testElasticsearchSingleton.stop();
				} catch (Exception e) {
					logger.info("The test Elasticsearch instance threw an exception during shutdown, probably due to multiple test contexts. This can be ignored.");
					logger.debug("The test Elasticsearch instance threw an exception during shutdown.", e);
				}
				if (installationDirectory != null && installationDirectory.exists()) {
					try {
						FileUtils.forceDelete(installationDirectory);
					} catch (IOException e) {
						logger.info("Error deleting the test Elasticsearch installation directory from temp {}", installationDirectory.getAbsolutePath());
					}
				}
			}
			testElasticsearchSingleton = null;
		}
	}

}
