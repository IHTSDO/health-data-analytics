package org.snomed.heathanalytics;

import java.util.Date;
import java.util.GregorianCalendar;

public class TestUtils {
	public static Date date(int year) {
		return date(year, 0, 1);
	}

	public static Date date(int year, int month, int dayOfMonth) {
		return new GregorianCalendar(year, month, dayOfMonth).getTime();
	}
}
