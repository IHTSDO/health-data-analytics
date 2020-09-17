package org.snomed.heathanalytics.server.store;

import org.junit.Test;
import org.snomed.heathanalytics.model.ClinicalEncounter;
import org.snomed.heathanalytics.model.Gender;
import org.snomed.heathanalytics.model.Patient;
import org.snomed.heathanalytics.server.AbstractDataTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PatientRepositoryTest extends AbstractDataTest {

	@Autowired
	private PatientRepository patientRepository;

	@Test
	// Test that all fields, particularly date fields, are working as expected.
	public void testMappings() {
		patientRepository.save(new Patient("123", new GregorianCalendar(1980, Calendar.APRIL, 10).getTime(), Gender.FEMALE)
				.addEncounter(new ClinicalEncounter(new GregorianCalendar(2000, Calendar.JULY, 5, 10, 15).getTime(), 123123L)));

		Optional<Patient> patientOptional = patientRepository.findById("123");

		assertTrue(patientOptional.isPresent());
		Patient patient = patientOptional.get();
		assertEquals(new GregorianCalendar(1980, Calendar.APRIL, 10).getTime().getTime(), patient.getDobLong());
		assertEquals(new GregorianCalendar(1980, Calendar.APRIL, 10).getTime(), patient.getDob());
		assertEquals(Gender.FEMALE, patient.getGender());
		Set<ClinicalEncounter> encounters = patient.getEncounters();
		assertEquals(1, encounters.size());
		ClinicalEncounter encounter = encounters.iterator().next();
		assertEquals(new GregorianCalendar(2000, Calendar.JULY, 5, 10, 15).getTime().getTime(), encounter.getDateLong());
		assertEquals(new GregorianCalendar(2000, Calendar.JULY, 5, 10, 15).getTime(), encounter.getDate());
		assertEquals(new Long(123123), encounter.getConceptId());
	}
}
