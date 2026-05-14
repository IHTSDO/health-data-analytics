package org.snomed.heathanalytics.server.config;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.support.HttpHeaders;

import java.time.Duration;
import java.util.*;

public class ElasticsearchConfig extends ElasticsearchConfiguration implements SmartInitializingSingleton {

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

	@Value("${elasticsearch.client.connect-timeout-seconds}")
	private long elasticsearchConnectTimeoutSeconds;

	@Value("${elasticsearch.client.socket-timeout-seconds}")
	private long elasticsearchSocketTimeoutSeconds;

	@Autowired
	private ElasticsearchProperties elasticsearchProperties;

	@Autowired
	private ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void afterSingletonsInstantiated() {
		initialiseIndices(elasticsearchOperationsProvider.getObject(), false);
	}

	@Override
	public ClientConfiguration clientConfiguration() {
		final String[] urls = Objects.requireNonNull(
				elasticsearchProperties.getUrls(),
				"elasticsearch.urls must be set");
		for (String url : urls) {
			logger.info("Elasticsearch host: {}", url);
		}
		return buildClientConfiguration(getHosts(urls), useHttps(urls));
	}

	/**
	 * Hosts are {@code host:port} strings as required by {@code ClientConfiguration.builder().connectedTo(...)}.
	 */
	protected ClientConfiguration buildClientConfiguration(String[] hosts, boolean useSsl) {
		HttpHeaders apiKeyHeaders = new HttpHeaders();
		ClientConfiguration.MaybeSecureClientConfigurationBuilder builder = ClientConfiguration.builder()
				.connectedTo(hosts);
		ClientConfiguration.TerminalClientConfigurationBuilder terminal = useSsl ? builder.usingSsl() : builder;
		return terminal
				.withDefaultHeaders(apiKeyHeaders)
				.withConnectTimeout(Duration.ofSeconds(elasticsearchConnectTimeoutSeconds))
				.withSocketTimeout(Duration.ofSeconds(elasticsearchSocketTimeoutSeconds))
				.withClientConfigurer(configureHttpClient())
				.build();
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
			int connectTimeoutSeconds = secondsToRequestConfigTimeout(elasticsearchConnectTimeoutSeconds);
			int socketTimeoutSeconds = secondsToRequestConfigTimeout(elasticsearchSocketTimeoutSeconds);
			clientBuilder.setRequestConfigCallback(builder -> {
				builder.setConnectionRequestTimeout(0);//Disable lease handling for the connection pool! See https://github.com/elastic/elasticsearch/issues/24069
				builder.setConnectTimeout(connectTimeoutSeconds * 1000);
				builder.setSocketTimeout(socketTimeoutSeconds * 1000);
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

	private static int secondsToRequestConfigTimeout(long seconds) {
		if (seconds > Integer.MAX_VALUE / 1000) {
			return Integer.MAX_VALUE / 1000;
		}
		return (int) seconds;
	}

	@Bean
	@Override
	public ElasticsearchCustomConversions elasticsearchCustomConversions() {
		return new ElasticsearchCustomConversions(Arrays.asList(new DateToLongConverter(), new LongToDateConverter()));
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
