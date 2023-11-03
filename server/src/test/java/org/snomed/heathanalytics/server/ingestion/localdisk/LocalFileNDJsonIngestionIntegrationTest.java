package org.snomed.heathanalytics.server.ingestion.localdisk;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.snomed.heathanalytics.model.ClinicalEvent;
import org.snomed.heathanalytics.model.Patient;
import org.snomed.heathanalytics.server.AbstractDataTest;
import org.snomed.heathanalytics.server.ingestion.elasticsearch.ElasticOutputStream;
import org.snomed.heathanalytics.server.store.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import java.io.File;
import java.util.*;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.snomed.heathanalytics.model.Gender.MALE;

public class LocalFileNDJsonIngestionIntegrationTest extends AbstractDataTest {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ElasticOutputStream elasticOutputStream;

	@Autowired
	private PatientRepository patientRepository;

	@Autowired
	private ElasticsearchOperations elasticsearchOperations;

	@Test
	public void testPatientIngestion() {
		new LocalFileNDJsonIngestionSource(objectMapper).stream(
				new LocalFileNDJsonIngestionSourceConfiguration(new File("src/test/resources/ingestion-test-population")), elasticOutputStream);

		// {"roleId":"0","gender":"MALE","dob":"19740528","events":[{"conceptId":195957006,"date":"20170910110011"}]}

		List<Patient> patients = patientRepository.findAll(Pageable.unpaged()).getContent();
		assertEquals(3, patients.size());
		Optional<Patient> firstPatientOptional = patients.stream().filter((p) -> p.getRoleId().equals("0")).findFirst();
		assertTrue(firstPatientOptional.isPresent());
		Patient patient = firstPatientOptional.get();
		assertEquals(MALE, patient.getGender());
		assertEquals(getUTCTime(1974, Calendar.MAY, 28, 0, 0, 0), patient.getDob());
		assertEquals(1, patient.getEvents().size());
		ClinicalEvent event = patient.getEvents().iterator().next();
		assertEquals(195957006L, event.getConceptId());
		assertEquals(getUTCTime(2017, Calendar.SEPTEMBER, 10, 11, 0, 11), event.getDate());
		String conceptDate = "195957006," + event.getDate().getTime();
		assertEquals(conceptDate, event.getConceptDate());
		SearchHits<Patient> hits = elasticsearchOperations.search(
				new NativeSearchQueryBuilder().withQuery(termQuery("events.conceptDate.keyword", conceptDate)).build(), Patient.class);
		assertEquals(1, hits.getTotalHits());
	}

	private Date getUTCTime(int year, int month, int dayOfMonth, int hour, int minute, int second) {
		GregorianCalendar gregorianCalendar = new GregorianCalendar(year, month, dayOfMonth);
		gregorianCalendar.setTimeZone(TimeZone.getTimeZone("UTC"));
		gregorianCalendar.set(Calendar.HOUR, hour);
		gregorianCalendar.set(Calendar.MINUTE, minute);
		gregorianCalendar.set(Calendar.SECOND, second);
		return gregorianCalendar.getTime();
	}
}
