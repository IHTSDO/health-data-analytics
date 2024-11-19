package org.snomed.heathanalytics.server.config;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import jakarta.annotation.PostConstruct;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.server.config.elasticsearch.DateToLongConverter;
import org.snomed.heathanalytics.server.config.elasticsearch.IndexNameProvider;
import org.snomed.heathanalytics.server.config.elasticsearch.LongToDateConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.support.HttpHeaders;

import java.util.*;

@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

	@Value("${elasticsearch.username}")
	private String elasticsearchUsername;

	@Value("${elasticsearch.password}")
	private String elasticsearchPassword;

	@Value("${elasticsearch.index.prefix}")
	private String indexNamePrefix;

	@Value("${elasticsearch.index.shards}")
	short indexShards;

	@Value("${elasticsearch.index.replicas}")
	short indexReplicas;

	@Autowired
	@Lazy
	private ElasticsearchOperations elasticsearchOperations;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@PostConstruct
	public void init() {
		initialiseIndices(elasticsearchOperations, false);
	}

	@Override
	public ClientConfiguration clientConfiguration() {
		final String[] urls = elasticsearchProperties().getUrls();
		for (String url : urls) {
			logger.info("Elasticsearch host: {}", url);
		}
		HttpHeaders apiKeyHeaders = new HttpHeaders();

		if (useHttps(urls)) {
			return ClientConfiguration.builder()
					.connectedTo(getHosts(urls))
					.usingSsl()
					.withDefaultHeaders(apiKeyHeaders)
					.withClientConfigurer(configureHttpClient())
					.build();
		} else {
			return ClientConfiguration.builder()
					.connectedTo(getHosts(urls))
					.withDefaultHeaders(apiKeyHeaders)
					.withClientConfigurer(configureHttpClient())
					.build();
		}
	}

	private boolean useHttps(String[] urls) {
		for (String url : urls) {
			if (url.startsWith("https://")) {
				return true;
			}
		}
		return false;
	}

	private ElasticsearchClients.ElasticsearchRestClientConfigurationCallback configureHttpClient() {
		return ElasticsearchClients.ElasticsearchRestClientConfigurationCallback.from(clientBuilder -> {
			clientBuilder.setRequestConfigCallback(builder -> {
				builder.setConnectionRequestTimeout(0);//Disable lease handling for the connection pool! See https://github.com/elastic/elasticsearch/issues/24069
				return builder;
			});
			final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			if (!Strings.isNullOrEmpty(elasticsearchUsername) && !Strings.isNullOrEmpty(elasticsearchPassword)) {
				credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(elasticsearchUsername, elasticsearchPassword));
			}
			clientBuilder.setHttpClientConfigCallback(httpClientBuilder -> {
				httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
				return httpClientBuilder;
			});
			return clientBuilder;
		});
	}

	private static String[] getHosts(String[] hosts) {
		List<HttpHost> httpHosts = new ArrayList<>();
		for (String host : hosts) {
			httpHosts.add(HttpHost.create(host));
		}
		return httpHosts.stream().map(HttpHost::toHostString).toList().toArray(new String[]{});
	}

	@Bean
	@Override
	public ElasticsearchCustomConversions elasticsearchCustomConversions() {
		return new ElasticsearchCustomConversions(Arrays.asList(new DateToLongConverter(), new LongToDateConverter()));
	}

	@Bean
	public ElasticsearchProperties elasticsearchProperties() {
		return new ElasticsearchProperties();
	}

	@Bean
	public IndexNameProvider indexNameProvider() {
		return new IndexNameProvider(indexNamePrefix);
	}

	protected void initialiseIndices(ElasticsearchOperations elasticsearchOperations, boolean deleteExisting) {
		Set<Class<?>> entities = scanForEntities("org.snomed.heathanalytics.server.model");
		logger.debug("Found {} entities to initialise", entities.size());
		// Initialise Elasticsearch indices
		Map<String, Object> settings = new HashMap<>();
		settings.put("index.number_of_shards", indexShards);
		settings.put("index.number_of_replicas", indexReplicas);
		initialiseIndexAndMappingForPersistentClasses(deleteExisting, elasticsearchOperations, settings, entities.toArray(new Class<?>[]{}));
	}

	public void initialiseIndexAndMappingForPersistentClasses(boolean deleteExisting, ElasticsearchOperations elasticsearchOperations, Map<String, Object> settings, Class<?>... persistentClass) {
		Set<Class<?>> classes = Sets.newHashSet(persistentClass);
		logger.info("Initialising {} indices", classes.size());
		if (deleteExisting) {
			logger.info("Deleting indices");

			for(Class<?> aClass : classes) {
				IndexCoordinates index = elasticsearchOperations.getIndexCoordinatesFor(aClass);
				logger.info("Deleting index {}", index.getIndexName());
				elasticsearchOperations.indexOps(index).delete();
			}
		}

		for(Class<?> aClass : classes) {
			IndexCoordinates index = elasticsearchOperations.getIndexCoordinatesFor(aClass);
			IndexOperations indexOperations = elasticsearchOperations.indexOps(index);
			if (!indexOperations.exists()) {
				logger.info("Creating index {}", index.getIndexName());
				if (settings != null && !settings.isEmpty()) {
					indexOperations.create(settings);
				} else {
					indexOperations.create(indexOperations.createSettings(aClass));
				}

				indexOperations.putMapping(indexOperations.createMapping(aClass));
			}
		}
	}
}
