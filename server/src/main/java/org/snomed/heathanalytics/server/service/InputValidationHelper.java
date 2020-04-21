package org.snomed.heathanalytics.server.service;

public class InputValidationHelper {
	public static void checkInput(String message, boolean shouldBeTrue) {
		if (!shouldBeTrue) {
			throw new IllegalArgumentException(message);
		}
	}
}
