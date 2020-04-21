package org.snomed.heathanalytics.server.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class CollectionUtils {

	public static <T> Stack<T> createStack(List<T> encounterCriteria) {
		Stack<T> encounterStack = new Stack<>();
		ArrayList<T> encounterCriteriaCopy = new ArrayList<>(encounterCriteria);
		Collections.reverse(encounterCriteriaCopy);
		encounterStack.addAll(encounterCriteriaCopy);
		return encounterStack;
	}
}
