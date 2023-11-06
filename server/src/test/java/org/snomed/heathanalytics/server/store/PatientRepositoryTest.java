package org.snomed.heathanalytics.server.store;

import org.junit.jupiter.api.Test;
import org.snomed.heathanalytics.model.ClinicalEvent;
import org.snomed.heathanalytics.model.Gender;
import org.snomed.heathanalytics.model.Patient;
import org.snomed.heathanalytics.server.AbstractDataTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class PatientRepositoryTest extends AbstractDataTest {

	@Autowired
	private PatientRepository patientRepository;

	@Test
	// Test that all fields, particularly date fields, are working as expected.
	public void testMappings() {
		Patient patientBeforeSave = new Patient("123", new GregorianCalendar(1980, Calendar.APRIL, 10).getTime(), Gender.FEMALE)
				.setCompositeRoleId("A|123")
				.addEvent(new ClinicalEvent(new GregorianCalendar(2000, Calendar.JULY, 5, 10, 15).getTime(), 123123L));
		patientRepository.save(patientBeforeSave);

		Optional<Patient> patientOptional = patientRepository.findById("A|123");

		assertTrue(patientOptional.isPresent());
		Patient patient = patientOptional.get();
		assertEquals(new GregorianCalendar(1980, Calendar.APRIL, 10).getTime().getTime(), patient.getDobLong());
		assertEquals(new GregorianCalendar(1980, Calendar.APRIL, 10).getTime(), patient.getDob());
		assertEquals(Gender.FEMALE, patient.getGender());
		Set<ClinicalEvent> events = patient.getEvents();
		assertEquals(1, events.size());
		ClinicalEvent event = events.iterator().next();
		assertEquals(new GregorianCalendar(2000, Calendar.JULY, 5, 10, 15).getTime().getTime(), event.getDateLong());
		assertEquals(new GregorianCalendar(2000, Calendar.JULY, 5, 10, 15).getTime(), event.getDate());
		assertEquals(123123L, event.getConceptId());
	}
}
