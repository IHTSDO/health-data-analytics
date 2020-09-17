package org.snomed.heathanalytics.server.ingestion;

import org.snomed.heathanalytics.model.ClinicalEncounter;
import org.snomed.heathanalytics.model.Patient;

import java.util.Collection;

public interface HealthDataOutputStream extends AutoCloseable {

	void createPatient(Patient patient);

	void createPatients(Collection<Patient> patients);

	void addClinicalEncounter(String roleId, ClinicalEncounter encounter);

	void close();
}
