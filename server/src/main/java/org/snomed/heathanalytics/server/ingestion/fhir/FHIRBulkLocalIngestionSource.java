package org.snomed.heathanalytics.server.ingestion.fhir;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.model.Gender;
import org.snomed.heathanalytics.model.Patient;
import org.snomed.heathanalytics.server.ingestion.HealthDataIngestionSource;
import org.snomed.heathanalytics.server.ingestion.HealthDataIngestionSourceConfiguration;
import org.snomed.heathanalytics.server.ingestion.HealthDataOutputStream;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FHIRBulkLocalIngestionSource implements HealthDataIngestionSource {

	private final ObjectMapper objectMapper;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public FHIRBulkLocalIngestionSource(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void stream(HealthDataIngestionSourceConfiguration configuration, HealthDataOutputStream healthDataOutputStream) {
		FHIRBulkLocalIngestionSourceConfiguration fhirConfiguration = (FHIRBulkLocalIngestionSourceConfiguration) configuration;
		File patientFile = fhirConfiguration.getPatientFile();
		logger.info("Reading Patients from {}.", patientFile.getPath());

		ObjectReader patientReader = objectMapper.readerFor(FHIRPatient.class);
		Date start = new Date();
		try {
			long read = 0;
			MappingIterator<FHIRPatient> patientIterator = patientReader.readValues(patientFile);
			for (UnmodifiableIterator<List<FHIRPatient>> it = Iterators.partition(patientIterator, ES_WRITE_BATCH_SIZE); it.hasNext(); ) {
				List<FHIRPatient> fhirPatients = it.next();
				List<Patient> patients = new ArrayList<>();
				for (FHIRPatient fhirPatient : fhirPatients) {
					patients.add(new Patient(fhirPatient.getId(), fhirPatient.getBirthDate(), Gender.from(fhirPatient.getGender())));
				}

				healthDataOutputStream.createPatients(patients);
				read += patients.size();
				if (read % 10_000 == 0) {
					logger.info("Consumed {} patients into store.", NumberFormat.getNumberInstance().format(read));
				}
			}
			logger.info("Read {} Patients from {} in {} seconds.", NumberFormat.getNumberInstance().format(read), patientFile.getPath(), (new Date().getTime() - start.getTime()) / 1_000);
		} catch (IOException e) {
			logger.error("Failed to read values from {}.", patientFile.getAbsolutePath(), e);
		}
	}

}
