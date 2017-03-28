package org.snomed.heathanalytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.domain.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import javax.annotation.PostConstruct;

@PropertySource("application-test.properties")
@SpringBootApplication
public class TestConfig {

	@Autowired
	private ElasticsearchOperations elasticsearchTemplate;

	private Logger logger = LoggerFactory.getLogger(getClass());

	@PostConstruct
	public void cleanUp() {
		logger.info("Deleting all existing entities before tests start");
		elasticsearchTemplate.deleteIndex(ClinicalEncounter.class);
		elasticsearchTemplate.deleteIndex(Patient.class);
	}

}
