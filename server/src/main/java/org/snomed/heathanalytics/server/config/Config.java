package org.snomed.heathanalytics.server.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication(
		exclude = {
				ElasticsearchDataAutoConfiguration.class,
				ElasticsearchRestClientAutoConfiguration.class
		}
)
@EnableElasticsearchRepositories(
		basePackages = {
				"org.snomed.heathanalytics.server.store"
		})
public abstract class Config {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Bean(name = { "elasticsearchOperations", "elasticsearchTemplate"})
	public ElasticsearchRestTemplate elasticsearchTemplate() {
		return new ElasticsearchRestTemplate(elasticsearchRestClient());
	}

	@Value("${spring.data.elasticsearch.cluster-nodes}")
	private String nodes;

	@Bean
	public RestHighLevelClient elasticsearchRestClient() {
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
		return new RestHighLevelClient(RestClient.builder(httpHosts.toArray(new HttpHost[]{})));
	}

	@Bean
	public ObjectMapper objectMapper() {
		return Jackson2ObjectMapperBuilder
				.json()
				.serializationInclusion(JsonInclude.Include.NON_NULL)
				.build();
	}

}
