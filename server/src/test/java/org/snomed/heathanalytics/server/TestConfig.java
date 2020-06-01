package org.snomed.heathanalytics.server;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.ihtsdo.otf.sqs.service.SnomedQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.server.store.PatientRepository;
import org.snomed.heathanalytics.server.store.SubsetRepository;
import org.snomed.heathanalytics.server.testutil.TestSnomedQueryServiceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.embeddedelasticsearch.PopularProperties;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;

@TestConfiguration
@PropertySource("application-test.properties")
@SpringBootApplication
public class TestConfig {

	private static final String ELASTICSEARCH_TEST_SERVER_VERSION = "6.8.7";
	private static final int PORT = 9932;

	private EmbeddedElastic testElasticsearchSingleton;
	private File installationDirectory;
	private boolean useLocalES = false;

	@Bean
	public SnomedQueryService snomedQueryService() throws IOException, ParseException {
		return TestSnomedQueryServiceBuilder.createBlank();
	}

	@Bean
	public ElasticsearchOperations elasticsearchTemplate(@Value("${test.elasticsearch.start-timeout-mins:2}") int unitTestElasticsearchStartTimeoutMins) {
		// Share the Elasticsearch instance between test contexts
		int port = PORT;

		if (testElasticsearchSingleton == null) {
			if (useLocalES) {
				port = 9200;
			} else {
				// Create and start a clean standalone Elasticsearch test instance
				String clusterName = "snowstorm-integration-test-cluster";

				try {
					installationDirectory = new File(System.getProperty("java.io.tmpdir"), "embedded-elasticsearch-temp-dir");
					File downloadDir = null;
					if (System.getProperty("user.home") != null) {
						downloadDir = new File(new File(System.getProperty("user.home"), "tmp"), "embedded-elasticsearch-download-cache");
						downloadDir.mkdirs();
					}
					LoggerFactory.getLogger(getClass()).info("Starting Elasticsearch node for unit tests. Timeout is {} minutes.", unitTestElasticsearchStartTimeoutMins);
					testElasticsearchSingleton = EmbeddedElastic.builder()
							.withElasticVersion(ELASTICSEARCH_TEST_SERVER_VERSION)
							.withStartTimeout(unitTestElasticsearchStartTimeoutMins, TimeUnit.MINUTES)
							.withSetting(PopularProperties.CLUSTER_NAME, clusterName)
							.withSetting(PopularProperties.HTTP_PORT, PORT)
							.withSetting("cluster.routing.allocation.disk.threshold_enabled", false)
							// Manually delete installation directory to prevent verbose error logging
							.withCleanInstallationDirectoryOnStop(false)
							.withDownloadDirectory(downloadDir)
							.withInstallationDirectory(installationDirectory)
							.build();
					testElasticsearchSingleton
							.start()
							.deleteIndices();
				} catch (InterruptedException | IOException e) {
					throw new RuntimeException("Failed to start standalone Elasticsearch instance.", e);
				}
			}
		}

		// Create client to to standalone instance
		return new ElasticsearchRestTemplate(new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", port))));
	}

	@PreDestroy
	public void shutdown() {
		synchronized (TestConfig.class) {
			Logger logger = LoggerFactory.getLogger(getClass());
			if (testElasticsearchSingleton != null) {
				if (!useLocalES) {
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
			}
			testElasticsearchSingleton = null;
		}
	}

}
