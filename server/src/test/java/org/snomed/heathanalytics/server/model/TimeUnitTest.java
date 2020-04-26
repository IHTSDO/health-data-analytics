package org.snomed.heathanalytics.server.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class TimeUnitTest {

	@Test
	public void test() {
		assertEquals(1_000, TimeUnit.SECOND.getMilliseconds());
		assertEquals(60000, TimeUnit.MINUTE.getMilliseconds());
		assertEquals(3600000, TimeUnit.HOUR.getMilliseconds());
		assertEquals(86400000, TimeUnit.DAY.getMilliseconds());
		assertEquals(604800000, TimeUnit.WEEK.getMilliseconds());
		assertEquals(2592000000L, TimeUnit.MONTH.getMilliseconds());
		assertEquals(31536000000L, TimeUnit.YEAR.getMilliseconds());
	}
}
