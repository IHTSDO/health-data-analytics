package org.snomed.heathanalytics.server;

import org.ihtsdo.otf.sqs.service.SnomedQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.model.Patient;
import org.snomed.heathanalytics.server.model.Subset;
import org.snomed.heathanalytics.server.store.PatientRepository;
import org.snomed.heathanalytics.server.store.SubsetRepository;
import org.snomed.heathanalytics.server.testutil.TestSnomedQueryServiceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.text.ParseException;

@PropertySource("application-test.properties")
@SpringBootApplication
public class TestConfig {

	@Autowired
	private PatientRepository patientRepository;

	@Autowired
	private SubsetRepository subsetRepository;

	@Bean
	public SnomedQueryService snomedQueryService() throws IOException, ParseException {
		return TestSnomedQueryServiceBuilder.createBlank();
	}

	@PreDestroy
	public void cleanUp() {
		patientRepository.deleteAll();
		subsetRepository.deleteAll();
	}

}
