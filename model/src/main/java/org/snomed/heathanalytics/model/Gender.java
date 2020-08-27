package org.snomed.heathanalytics.model;

public enum Gender {
	MALE, FEMALE;

	public static Gender from(String g) {
		for (Gender gender : Gender.values()) {
			if (gender.name().equalsIgnoreCase(g)) {
				return gender;
			}
		}
		return null;
	}
}
