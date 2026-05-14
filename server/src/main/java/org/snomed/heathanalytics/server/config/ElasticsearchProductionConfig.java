package org.snomed.heathanalytics.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
@Import(ElasticsearchConfig.class)
public class ElasticsearchProductionConfig {
}
