package org.snomed.heathanalytics.datageneration;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

final class DateUtil {

	static Date dateOfBirthFromAge(int ageInYears) {
		GregorianCalendar date = new GregorianCalendar();
		date.add(Calendar.YEAR, -ageInYears);
		clearTime(date);
		return date.getTime();
	}

	private static void clearTime(GregorianCalendar calendar) {
		calendar.clear(Calendar.HOUR);
		calendar.clear(Calendar.MINUTE);
		calendar.clear(Calendar.SECOND);
		calendar.clear(Calendar.MILLISECOND);
	}
}
