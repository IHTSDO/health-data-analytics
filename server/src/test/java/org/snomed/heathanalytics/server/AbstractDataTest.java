package org.snomed.heathanalytics.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.snomed.heathanalytics.server.store.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith(SpringExtension.class)
@Testcontainers
@ContextConfiguration(classes = TestConfig.class)
public abstract class AbstractDataTest {

	@Autowired
	private PatientRepository patientRepository;

	@AfterEach
	public void clearIndexes() {
		patientRepository.deleteAll();
	}

}
