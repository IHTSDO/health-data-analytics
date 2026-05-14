package org.snomed.heathanalytics.server.config;

import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

/**
 * {@link ElasticsearchConfig} is registered only via {@link ElasticsearchProductionConfig} {@code @Import}
 * (production) or subclass {@link org.snomed.heathanalytics.server.TestConfig} (tests); it must not also be
 * picked up by classpath scanning or beans and {@code @Configuration} proxy semantics break.
 */
public final class ExcludeElasticsearchConfigFromComponentScan implements TypeFilter {

	@Override
	public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
		return ElasticsearchConfig.class.getName().equals(metadataReader.getClassMetadata().getClassName());
	}
}
