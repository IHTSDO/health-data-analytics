package org.snomed.heathanalytics.service;

import java.util.*;

public class CollectionUtils {

	public static <T> Stack<T> createStack(List<T> encounterCriteria) {
		Stack<T> encounterStack = new Stack<>();
		ArrayList<T> encounterCriteriaCopy = new ArrayList<>(encounterCriteria);
		Collections.reverse(encounterCriteriaCopy);
		encounterStack.addAll(encounterCriteriaCopy);
		return encounterStack;
	}
}
