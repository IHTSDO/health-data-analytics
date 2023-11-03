package org.snomed.heathanalytics.server.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class CollectionUtils {

	public static <T> Stack<T> createStack(List<T> eventCriteria) {
		Stack<T> eventStack = new Stack<>();
		ArrayList<T> eventCriteriaCopy = new ArrayList<>(eventCriteria);
		Collections.reverse(eventCriteriaCopy);
		eventStack.addAll(eventCriteriaCopy);
		return eventStack;
	}

	public static <T> Iterable<T> orEmpty(Iterable<T> collection) {
		return collection != null ? collection : Collections.emptySet();
	}
}
