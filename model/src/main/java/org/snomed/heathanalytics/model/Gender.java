package org.snomed.heathanalytics.model;

public enum Gender {
	MALE, FEMALE, UNKNOWN;

	public static Gender from(String g) {
		for (Gender gender : Gender.values()) {
			if (gender.name().equalsIgnoreCase(g)) {
				return gender;
			}
		}
		return null;
	}
}
