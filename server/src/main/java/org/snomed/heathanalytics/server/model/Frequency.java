package org.snomed.heathanalytics.server.model;

public class Frequency {

	private Integer minRepetitions;
	private Integer minTimeBetween;
	private Integer maxTimeBetween;
	private TimeUnit timeUnit;

	public Frequency() {
	}

	public Frequency(Integer minRepetitions, Integer minTimeBetween, Integer maxTimeBetween, TimeUnit timeUnit) {
		this.minRepetitions = minRepetitions;
		this.minTimeBetween = minTimeBetween;
		this.maxTimeBetween = maxTimeBetween;
		this.timeUnit = timeUnit;
	}

	public Integer getMinRepetitions() {
		return minRepetitions;
	}

	public Integer getMinTimeBetween() {
		return minTimeBetween;
	}

	public Integer getMaxTimeBetween() {
		return maxTimeBetween;
	}

	public TimeUnit getTimeUnit() {
		return timeUnit;
	}

}
