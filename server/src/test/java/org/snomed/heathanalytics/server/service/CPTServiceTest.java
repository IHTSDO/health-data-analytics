package org.snomed.heathanalytics.server.service;

import org.junit.Test;
import org.snomed.heathanalytics.server.model.CPTCode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.junit.Assert.*;

public class CPTServiceTest {

	@Test
	public void test() throws IOException {
		CPTService cptService = new CPTService("src/test/resources/dummy-cpt-codes");
		cptService.attemptToLoadCPTDataFiles();
		Set<CPTCode> cptCodes = cptService.getCPTCodes();
		assertNotNull(cptCodes);
		assertEquals(2, cptCodes.size());
	}

}
