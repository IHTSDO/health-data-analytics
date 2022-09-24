package org.snomed.heathanalytics.server;

import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.server.config.Config;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;

@PropertySource("application.properties")
@PropertySource("application-test.properties")
@TestConfiguration
@SpringBootApplication(
		exclude = {
				ElasticsearchRestClientAutoConfiguration.class,
				ElasticsearchDataAutoConfiguration.class
		})
public class TestConfig extends Config {

	private static final String ELASTIC_SEARCH_SERVER_VERSION = "7.10.2";

	// set it to true to use local instance instead of test container
	static final boolean useLocalElasticsearch = false;

	private static final Logger LOGGER = LoggerFactory.getLogger(TestConfig.class);

	@Container
	private static final ElasticsearchContainer elasticsearchContainer;
	static
	{
		if (useLocalElasticsearch) {
			elasticsearchContainer = null;
		} else {
			if (!DockerClientFactory.instance().isDockerAvailable()) {
				LOGGER.error("No docker client available to run integration tests.");
				LOGGER.info("Integration tests use the TestContainers framework.(https://www.testcontainers.org)");
				LOGGER.info("TestContainers framework requires docker to be installed.(https://www.testcontainers.org/supported_docker_environment)");
				LOGGER.info("You can download docker via (https://docs.docker.com/get-docker)");
				System.exit(-1);
			}
			elasticsearchContainer = new CustomElasticsearchContainer();
			elasticsearchContainer.start();
		}
	}

	public static final String DEFAULT_LANGUAGE_CODE = "en";

	public static class CustomElasticsearchContainer extends ElasticsearchContainer {
		public CustomElasticsearchContainer() {
			super("docker.elastic.co/elasticsearch/elasticsearch:" + ELASTIC_SEARCH_SERVER_VERSION);
			// these are mapped ports used by the test container the actual ports used might be different
			this.addFixedExposedPort(9235, 9235);
			this.addFixedExposedPort(9330, 9330);
			this.addEnv("cluster.name", "integration-test-cluster");
		}
	}

	static ElasticsearchContainer getElasticsearchContainerInstance() {
		return elasticsearchContainer;
	}

	@Override
	@Bean
	public RestHighLevelClient elasticsearchRestClient() {
		if (!useLocalElasticsearch) {
			System.out.println("Connecting to " + elasticsearchContainer.getHttpHostAddress());
			return RestClients.create(ClientConfiguration.builder()
					.connectedTo(elasticsearchContainer.getHttpHostAddress()).build()).rest();
		}
		return RestClients.create(ClientConfiguration.builder()
				.connectedTo("localhost:9200").build()).rest();
	}
}
