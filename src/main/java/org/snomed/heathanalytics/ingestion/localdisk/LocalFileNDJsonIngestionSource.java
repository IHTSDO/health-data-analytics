package org.snomed.heathanalytics.ingestion.localdisk;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.ingestion.HealthDataIngestionSource;
import org.snomed.heathanalytics.ingestion.HealthDataIngestionSourceConfiguration;
import org.snomed.heathanalytics.ingestion.HealthDataOutputStream;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class LocalFileNDJsonIngestionSource implements HealthDataIngestionSource {

	private ObjectMapper objectMapper;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public LocalFileNDJsonIngestionSource(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void stream(HealthDataIngestionSourceConfiguration configuration, HealthDataOutputStream healthDataOutputStream) {
		LocalFileNDJsonIngestionSourceConfiguration config = (LocalFileNDJsonIngestionSourceConfiguration) configuration;
		File ndJsonDirectory = config.getFileDirectory();
		File[] files = ndJsonDirectory.listFiles((dir, name) -> name.endsWith(".ndjson"));
		if (files != null) {
			ObjectReader patientReader = objectMapper.readerFor(Patient.class);
			for (File ndJsonFile : files) {
				Date start = new Date();
				logger.info("Reading Patients from {}.", ndJsonFile.getPath());
				try {
					long read = 0;
					MappingIterator<Patient> patientIterator = patientReader.readValues(ndJsonFile);
					while (patientIterator.hasNext()) {
						healthDataOutputStream.createPatient(patientIterator.next());
						read++;
						if (read % 1_000 == 0) {
							logger.info("Consumed {} patients into store.", read);
						}
					}
					logger.info("Read {} Patients from {} in {} seconds.", read, ndJsonFile.getPath(), (new Date().getTime() - start.getTime()) / 1_000);
				} catch (IOException e) {
					logger.error("Failed to read values from {}.", ndJsonFile.getAbsolutePath(), e);
				}
			}
		}
	}
}
