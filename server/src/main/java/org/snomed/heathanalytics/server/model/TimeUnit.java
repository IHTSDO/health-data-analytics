package org.snomed.heathanalytics.server.model;

public enum TimeUnit {

	SECOND(1_000),
	MINUTE(SECOND.milliseconds * 60),
	HOUR(MINUTE.milliseconds * 60),
	DAY(HOUR.milliseconds * 24),
	WEEK(DAY.milliseconds * 7),
	MONTH(DAY.milliseconds * 30),
	YEAR(DAY.milliseconds * 365);

	private final long milliseconds;

	TimeUnit(long milliseconds) {
		this.milliseconds = milliseconds;
	}

	public long getMilliseconds() {
		return milliseconds;
	}

}
