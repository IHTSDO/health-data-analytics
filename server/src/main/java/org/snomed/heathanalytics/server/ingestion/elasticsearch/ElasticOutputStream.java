package org.snomed.heathanalytics.server.ingestion.elasticsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.model.ClinicalEncounter;
import org.snomed.heathanalytics.model.Patient;
import org.snomed.heathanalytics.server.ingestion.HealthDataOutputStream;
import org.snomed.heathanalytics.server.store.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class ElasticOutputStream implements HealthDataOutputStream {

	private PatientRepository patientRepository;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public ElasticOutputStream(PatientRepository patientRepository) {
		this.patientRepository = patientRepository;
	}

	@Override
	public void createPatient(Patient patient) {
		patientRepository.save(patient);
	}

	@Override
	public void createPatients(Collection<Patient> patients) {
		patientRepository.save(patients);
	}

	@Override
	public void addClinicalEncounter(String roleId, ClinicalEncounter encounter) {
		Patient patient = patientRepository.findOne(roleId);
		if (patient != null) {
			patient.addEncounter(encounter);
			patientRepository.save(patient);
		} else {
			logger.error("Failed to add clinical encounter {}/{} - patient not found with id {}", encounter.getDate(), encounter.getConceptId(), roleId);
		}
	}

}
