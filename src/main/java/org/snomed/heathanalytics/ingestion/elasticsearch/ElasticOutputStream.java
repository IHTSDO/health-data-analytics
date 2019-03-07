package org.snomed.heathanalytics.ingestion.elasticsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.ingestion.HealthDataOutputStream;
import org.snomed.heathanalytics.store.PatientRepository;
import org.springframework.stereotype.Service;

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
	public void addClinicalEncounter(String roleId, ClinicalEncounter encounter) {
		Patient patient = patientRepository.findById(roleId).orElse(null);
		if (patient != null) {
			patient.addEncounter(encounter);
			patientRepository.save(patient);
		} else {
			logger.error("Failed to add clinical encounter {}/{} - patient not found with id {}", encounter.getDate(), encounter.getConceptId(), roleId);
		}
	}

}
