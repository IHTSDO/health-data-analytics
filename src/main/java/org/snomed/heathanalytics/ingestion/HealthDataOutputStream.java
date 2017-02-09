package org.snomed.heathanalytics.ingestion;

import org.snomed.heathanalytics.domain.Sex;

import java.util.Date;

public interface HealthDataOutputStream {

	String createPatient(String name, Date dateOfBirth, Sex sex);

	void addCondition(String roleId, String conceptId);
}
