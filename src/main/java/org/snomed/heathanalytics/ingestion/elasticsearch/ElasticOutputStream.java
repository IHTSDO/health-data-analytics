package org.snomed.heathanalytics.ingestion.elasticsearch;

import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.domain.ClinicalEncounterType;
import org.snomed.heathanalytics.domain.Gender;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.ingestion.HealthDataOutputStream;
import org.snomed.heathanalytics.store.ClinicalEncounterRepository;
import org.snomed.heathanalytics.store.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class ElasticOutputStream implements HealthDataOutputStream {

	private PatientRepository patientRepository;

	private ClinicalEncounterRepository clinicalEncounterRepository;

	public ElasticOutputStream(PatientRepository patientRepository, ClinicalEncounterRepository clinicalEncounterRepository) {
		this.patientRepository = patientRepository;
		this.clinicalEncounterRepository = clinicalEncounterRepository;
	}

	@Override
	public void createPatient(String roleId, String name, Date dateOfBirth, Gender gender) {
		patientRepository.save(new Patient(roleId, name, dateOfBirth, gender));
	}

	@Override
	public void addClinicalEncounter(String roleId, Date date, ClinicalEncounterType type, Long conceptId) {
		clinicalEncounterRepository.save(new ClinicalEncounter(roleId, date, type, conceptId));
	}
}
