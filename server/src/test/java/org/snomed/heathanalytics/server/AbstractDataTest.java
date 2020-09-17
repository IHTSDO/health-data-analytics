package org.snomed.heathanalytics.server;

import org.junit.After;
import org.junit.runner.RunWith;
import org.snomed.heathanalytics.server.store.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public abstract class AbstractDataTest {

	@Autowired
	private PatientRepository patientRepository;

	@After
	public void clearIndexes() {
		patientRepository.deleteAll();
	}

}
