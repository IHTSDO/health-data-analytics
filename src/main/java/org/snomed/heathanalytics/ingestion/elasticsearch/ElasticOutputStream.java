package org.snomed.heathanalytics.ingestion.elasticsearch;

import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.domain.Sex;
import org.snomed.heathanalytics.ingestion.HealthDataOutputStream;
import org.snomed.heathanalytics.snomed.SnomedSubsumptionService;
import org.snomed.heathanalytics.store.ClinicalEncounterRepository;
import org.snomed.heathanalytics.store.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Set;

@Service
public class ElasticOutputStream implements HealthDataOutputStream {

	private PatientRepository patientRepository;

	private ClinicalEncounterRepository clinicalEncounterRepository;

	@Autowired
	private SnomedSubsumptionService snomedSubsumptionService;

	public ElasticOutputStream(PatientRepository patientRepository, ClinicalEncounterRepository clinicalEncounterRepository) {
		this.patientRepository = patientRepository;
		this.clinicalEncounterRepository = clinicalEncounterRepository;
	}

	@Override
	public void createPatient(String roleId, String name, Date dateOfBirth, Sex sex) {
		patientRepository.save(new Patient(roleId, name, dateOfBirth, sex));
	}

	@Override
	public void addClinicalEncounter(String roleId, Date date, String conceptId) {
		Set<Long> ancestors = snomedSubsumptionService.getAncestorsOf(Long.parseLong(conceptId));
		clinicalEncounterRepository.save(new ClinicalEncounter(roleId, date, conceptId, ancestors));
	}
}
