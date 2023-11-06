package org.snomed.heathanalytics.server.ingestion.elasticsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.model.ClinicalEvent;
import org.snomed.heathanalytics.model.Patient;
import org.snomed.heathanalytics.server.ingestion.HealthDataOutputStream;
import org.snomed.heathanalytics.server.store.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

@Service
public class ElasticOutputStream implements HealthDataOutputStream {

	private final PatientRepository patientRepository;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final Map<String, Patient> patientBuffer = new HashMap<>();

	public ElasticOutputStream(PatientRepository patientRepository) {
		this.patientRepository = patientRepository;
	}

	@Override
	public void createPatient(Patient patient, String dataset) {
		patient.setCompositeRoleId(getCompositeRoleId(dataset, patient.getRoleId()));
		patient.setDataset(dataset);
		patientRepository.save(patient);
	}

	@Override
	public void createPatients(Collection<Patient> patients, String dataset) {
		patients.forEach(patient -> {
			patient.setCompositeRoleId(getCompositeRoleId(dataset, patient.getRoleId()));
			patient.setDataset(dataset);
		});
		patientRepository.saveAll(patients);
	}

	private static String getCompositeRoleId(String dataset, String roleId) {
		return format("%s|%s", dataset, roleId);
	}

	@Override
	public void addClinicalEvent(String roleId, ClinicalEvent event, String dataset) {
		String compositeRoleId = getCompositeRoleId(dataset, roleId);
		Patient patient = patientBuffer.get(compositeRoleId);
		if (patient == null) {
			Optional<Patient> patientOptional = patientRepository.findById(compositeRoleId);
			if (patientOptional.isPresent()) {
				patient = patientOptional.get();
				patientBuffer.put(compositeRoleId, patient);
			}
		}
		if (patient != null) {
			patient.addEvent(event);
			if (patientBuffer.size() == 10_000) {
				flush();
			}
		} else {
			logger.error("Failed to add clinical event {}/{} - patient not found with id {}", event.getDate(), event.getConceptId(), compositeRoleId);
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
