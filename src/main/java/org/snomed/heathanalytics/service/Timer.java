package org.snomed.heathanalytics.service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class Timer {

	private Map<String, Long> times;
	private final Date start;
	private Date split;

	public Timer() {
		times = new LinkedHashMap<>();
		start = new Date();
		split = start;
	}

	public void split(String label) {
		Date now = new Date();
		times.put(label, now.getTime() - split.getTime());
		split = now;
	}

	public Map<String, Long> getTimes() {
		times.put("total", new Date().getTime() - start.getTime());
		return times;
	}
}
