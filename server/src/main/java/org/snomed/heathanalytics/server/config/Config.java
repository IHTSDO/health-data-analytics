package org.snomed.heathanalytics.server.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.info.BuildProperties;
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

	@Bean(name = { "elasticsearchOperations", "elasticsearchTemplate"})
	public ElasticsearchRestTemplate elasticsearchTemplate() {
		return new ElasticsearchRestTemplate(elasticsearchRestClient());
	}

	@Value("${spring.data.elasticsearch.cluster-nodes}")
	private String nodes;

	@Autowired(required = false)
	private BuildProperties buildProperties;

	private final Logger logger = LoggerFactory.getLogger(getClass());

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

	@Bean
	public OpenAPI apiInfo() {
		final String version = buildProperties != null ? buildProperties.getVersion() : "DEV";
		return new OpenAPI()
				.info(new Info().title("Snolytical")
						.description("Healthcare Analytics Demonstrator using SNOMED CT")
						.version(version)
						.contact(new Contact().name("SNOMED International").url("https://www.snomed.org"))
						.license(new License().name("Apache 2.0").url("http://www.apache.org/licenses/LICENSE-2.0")))
				.externalDocs(new ExternalDocumentation().description("See more about Snolytical in GitHub").url("https://github.com/IHTSDO/health-data-analytics"));
	}

}
