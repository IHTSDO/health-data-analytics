package org.snomed.heathanalytics.server.service;

import org.junit.Test;
import org.snomed.heathanalytics.server.model.CPTCode;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;

public class CPTServiceTest {

	@Test
	public void test() throws IOException {
		CPTService cptService = new CPTService("src/test/resources/dummy-cpt-codes");

		cptService.attemptToLoadCPTDataFiles();

		Map<String, CPTCode> cptCodes = cptService.getCptCodeMap();
		assertNotNull(cptCodes);
		assertEquals(2, cptCodes.size());

		Map<String, CPTCode> snomedToCptMap = cptService.getSnomedToCptMap();
		assertNotNull(snomedToCptMap);
		assertEquals(3, snomedToCptMap.size());

		for (String cptCode : cptCodes.keySet()) {
			assertTrue(String.format("Code %s found in loaded map.", cptCode), snomedToCptMap.values().stream().anyMatch(code -> code.getCptCode().equals(cptCode)));
		}

		assertEquals("12345", snomedToCptMap.get("268547008").getCptCode());
		assertEquals("12345", snomedToCptMap.get("12346001").getCptCode());
		assertEquals("20123", snomedToCptMap.get("12347001").getCptCode());
	}

}
