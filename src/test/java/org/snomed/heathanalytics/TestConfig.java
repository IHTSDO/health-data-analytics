package org.snomed.heathanalytics;

import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.sqs.service.SnomedQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.store.PatientRepository;
import org.snomed.heathanalytics.testutil.TestSnomedQueryServiceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.text.ParseException;

@PropertySource("application-test.properties")
@SpringBootApplication
public class TestConfig {

	@Autowired
	private ElasticsearchOperations elasticsearchTemplate;

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Bean
	public SnomedQueryService snomedQueryService() throws IOException, ParseException {
		return TestSnomedQueryServiceBuilder.createBlank();
	}

	@PostConstruct
	public void cleanUp() {
		logger.info("Deleting all existing entities before tests start");
		elasticsearchTemplate.deleteIndex(Patient.class);
	}

}
