package org.snomed.heathanalytics.server;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class TestUtils {
	public static Date date(int year) {
		return date(year, 0, 1);
	}

	public static Date date(int year, int month, int dayOfMonth) {
		return new GregorianCalendar(year, month, dayOfMonth).getTime();
	}

	public static Date getDob(int years) {
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.clear(Calendar.HOUR);
		calendar.clear(Calendar.MINUTE);
		calendar.clear(Calendar.SECOND);
		calendar.clear(Calendar.MILLISECOND);
		calendar.add(Calendar.YEAR, -years);
		return calendar.getTime();
	}
}
