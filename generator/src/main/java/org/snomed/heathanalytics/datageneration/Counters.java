package org.snomed.heathanalytics.datageneration;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

public class Counters {

	private final Map<String, AtomicLong> counters = new TreeMap<>();

	public void inc(String name) {
		counters.computeIfAbsent(name, (k) -> new AtomicLong()).incrementAndGet();
	}

	public void printAll() {
		System.out.println("- Counters -");
		for (String key : counters.keySet()) {
			System.out.println(key + " : " + counters.get(key).get());
		}
		System.out.println("---");
	}
}
