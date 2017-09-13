package org.snomed.heathanalytics.ingestion;

import org.snomed.heathanalytics.domain.ClinicalEncounterType;
import org.snomed.heathanalytics.domain.Gender;

import java.util.Date;

public interface HealthDataOutputStream {

	void createPatient(String roleId, String name, Date dateOfBirth, Gender gender);

	void addClinicalEncounter(String roleId, Date date, ClinicalEncounterType type, Long conceptId);
}
