package org.snomed.heathanalytics.server.service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class Timer {

	private Map<String, Float> times;
	private final Date start;
	private Date split;

	public Timer() {
		times = new LinkedHashMap<>();
		start = new Date();
		split = start;
	}

	public void split(String label) {
		Date now = new Date();
		times.put(label, (now.getTime() - split.getTime()) / 1000f);
		split = now;
	}

	public Map<String, Float> getTimes() {
		times.put("total", (new Date().getTime() - start.getTime()) / 1000f);
		return times;
	}
}
