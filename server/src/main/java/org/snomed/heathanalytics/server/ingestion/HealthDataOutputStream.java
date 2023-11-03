package org.snomed.heathanalytics.server.ingestion;

import org.snomed.heathanalytics.model.ClinicalEvent;
import org.snomed.heathanalytics.model.Patient;

import java.util.Collection;

public interface HealthDataOutputStream extends AutoCloseable {

	void createPatient(Patient patient);

	void createPatients(Collection<Patient> patients);

	void addClinicalEvent(String roleId, ClinicalEvent event);

	void close();
}
