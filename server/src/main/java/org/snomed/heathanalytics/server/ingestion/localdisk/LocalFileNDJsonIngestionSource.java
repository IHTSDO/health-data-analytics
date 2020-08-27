package org.snomed.heathanalytics.server.ingestion.localdisk;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.model.Patient;
import org.snomed.heathanalytics.server.ingestion.HealthDataIngestionSource;
import org.snomed.heathanalytics.server.ingestion.HealthDataIngestionSourceConfiguration;
import org.snomed.heathanalytics.server.ingestion.HealthDataOutputStream;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;

public class LocalFileNDJsonIngestionSource implements HealthDataIngestionSource {

	private final ObjectMapper objectMapper;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public LocalFileNDJsonIngestionSource(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

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
					for (UnmodifiableIterator<List<Patient>> it = Iterators.partition(patientIterator, ES_WRITE_BATCH_SIZE); it.hasNext(); ) {
						List<Patient> patients = it.next();
						healthDataOutputStream.createPatients(patients);
						read += patients.size();
						if (read % 10_000 == 0) {
							logger.info("Consumed {} patients into store.", NumberFormat.getNumberInstance().format(read));
						}
					}
					logger.info("Read {} Patients from {} in {} seconds.", NumberFormat.getNumberInstance().format(read), ndJsonFile.getPath(), (new Date().getTime() - start.getTime()) / 1_000);
				} catch (IOException e) {
					logger.error("Failed to read values from {}.", ndJsonFile.getAbsolutePath(), e);
				}
			}
		}
	}
}
