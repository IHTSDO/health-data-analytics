package org.snomed.heathanalytics.ingestion;

import org.snomed.heathanalytics.domain.Sex;

import java.util.Date;

public interface HealthDataOutputStream {

	void createPatient(String roleId, String name, Date dateOfBirth, Sex sex);

	void addClinicalEncounter(String roleId, Date date, String conceptId);
}
