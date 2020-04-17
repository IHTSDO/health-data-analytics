package org.snomed.heathanalytics.ingestion;

import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.domain.Patient;

import java.util.Collection;

public interface HealthDataOutputStream {

	void createPatient(Patient patient);

	void createPatients(Collection<Patient> patients);

	void addClinicalEncounter(String roleId, ClinicalEncounter encounter);
}
