package org.snomed.heathanalytics.server.ingestion.elasticsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.model.ClinicalEncounter;
import org.snomed.heathanalytics.model.Patient;
import org.snomed.heathanalytics.server.ingestion.HealthDataOutputStream;
import org.snomed.heathanalytics.server.store.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ElasticOutputStream implements HealthDataOutputStream {

	private final PatientRepository patientRepository;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final Map<String, Patient> patientBuffer = new HashMap<>();

	public ElasticOutputStream(PatientRepository patientRepository) {
		this.patientRepository = patientRepository;
	}

	@Override
	public void createPatient(Patient patient) {
		patientRepository.save(patient);
	}

	@Override
	public void createPatients(Collection<Patient> patients) {
		patientRepository.saveAll(patients);
	}

	@Override
	public void addClinicalEncounter(String roleId, ClinicalEncounter encounter) {
		Patient patient = patientBuffer.get(roleId);
		if (patient == null) {
			Optional<Patient> patientOptional = patientRepository.findById(roleId);
			if (patientOptional.isPresent()) {
				patient = patientOptional.get();
				patientBuffer.put(roleId, patient);
			}
		}
		if (patient != null) {
			patient.addEncounter(encounter);
			if (patientBuffer.size() == 10_000) {
				flush();
			}
		} else {
			logger.error("Failed to add clinical encounter {}/{} - patient not found with id {}", encounter.getDate(), encounter.getConceptId(), roleId);
		}
	}

	@Override
	public void close() {
		flush();
	}

	public void flush() {
		if (!patientBuffer.isEmpty()) {
			patientRepository.saveAll(patientBuffer.values());
			patientBuffer.clear();
		}
	}
}
