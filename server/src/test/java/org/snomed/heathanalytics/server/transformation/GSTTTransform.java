package org.snomed.heathanalytics.server.transformation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import org.snomed.heathanalytics.server.ingestion.fhir.*;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class GSTTTransform {

	public static void main(String[] args) throws IOException {
		new GSTTTransform().transform("../../data/gstt/diagnoses.txt", "../../data/gstt/medications.txt");
	}

	// 07/01/2014 08:49
	private static final SimpleDateFormat INPUT_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy hh:mm");
	// 1967-09-03
	private static final SimpleDateFormat FHIR_DOB_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	private static final Long YEAR_IN_MILLISECONDS = 31556952000L;

	private final Set<String> patientIds = new HashSet<>();

	private void transform(String diagnosesFile, String medicationsFile) throws IOException {
		try (
				final BufferedReader diagnosesTsv = new BufferedReader(new InputStreamReader(new FileInputStream(diagnosesFile)));
				final BufferedReader medicationsTsv = new BufferedReader(new InputStreamReader(new FileInputStream(medicationsFile)));
				final FileWriter fhirPatients = new FileWriter("gstt-fhir-resources/Patient.ndjson");
				final FileWriter fhirConditions = new FileWriter("gstt-fhir-resources/Condition.ndjson");
				final FileWriter fhirMedications = new FileWriter("gstt-fhir-resources/MedicationRequest.ndjson")) {

			final SequenceWriter patientWriter = new ObjectMapper()
					.writerFor(FHIRPatient.class)
					.withRootValueSeparator("\n")
					.writeValues(fhirPatients);

			final SequenceWriter conditionWriter = new ObjectMapper()
					.writerFor(FHIRCondition.class)
					.withRootValueSeparator("\n")
					.writeValues(fhirConditions);

			final SequenceWriter medicationWriter = new ObjectMapper()
					.writerFor(FHIRMedicationRequest.class)
					.withRootValueSeparator("\n")
					.writeValues(fhirMedications);

			String line;

			long patients = 0;
			long conditions = 0;
			long medications = 0;

			diagnosesTsv.readLine();// Discard header
			while ((line = diagnosesTsv.readLine()) != null && !line.isEmpty()) {
				// SpellNBRPsuedo	TrustIDPsuedo	SCTID	SCTFSNDSC	GenderDSC	CurrentAge	AdmitDTS
				// 0				1				2		3			4			5			6
				final String[] diagnosesColumns = line.split("\\t");
				final String patientNumber = diagnosesColumns[1];
				try {
					final long admitTime = INPUT_DATE_FORMAT.parse(diagnosesColumns[6]).getTime();
					if (patientIds.add(patientNumber)) {
						// New patient
						addNewPatient(patientNumber, admitTime, diagnosesColumns, patientWriter);
						patients++;
					}

					final FHIRCondition fhirCondition = new FHIRCondition(
							FHIRReference.patient(patientNumber),
							FHIRCodeableConcept.snomedConcept(diagnosesColumns[2]),
							FHIRCodeableConcept.CLINICAL_STATUS_ACTIVE,
							FHIRCodeableConcept.VERIFICATION_STATUS_CONFIRMED,
							new Date(admitTime));
					conditionWriter.write(fhirCondition);
					conditions++;
				} catch (ParseException e) {
					System.err.println("Failed to parse dates for patient " + patientNumber);
					System.err.println(diagnosesColumns.length + " - " + Arrays.toString(diagnosesColumns));
				}
			}


			medicationsTsv.readLine();// Discard header
			while ((line = medicationsTsv.readLine()) != null && !line.isEmpty()) {
				// SpellNBRPsuedo	TrustIDPsuedo	SCTID	SCTFSNDSC	GenderDSC	CurrentAge	AdmitDTS
				// 0				1				2		3			4			5			6
				final String[] medicationsColumns = line.split("\\t");
				final String patientNumber = medicationsColumns[1];
				try {
					final long admitTime = INPUT_DATE_FORMAT.parse(medicationsColumns[6]).getTime();
					if (patientIds.add(patientNumber)) {
						// New patient
						addNewPatient(patientNumber, admitTime, medicationsColumns, patientWriter);
						patients++;
					}

					final FHIRMedicationRequest fhirMedicationRequest = new FHIRMedicationRequest(
							FHIRReference.patient(patientNumber),
							FHIRCodeableConcept.snomedConcept(medicationsColumns[2]),
							"active",
							"order",
							new Date(admitTime));
					medicationWriter.write(fhirMedicationRequest);
					medications++;
				} catch (ParseException e) {
					System.err.println("Failed to parse dates for patient " + patientNumber);
					System.err.println(medicationsColumns.length + " - " + Arrays.toString(medicationsColumns));
				}
			}

			patientWriter.flush();
			patientWriter.close();

			conditionWriter.flush();
			conditionWriter.close();

			medicationWriter.flush();
			medicationWriter.close();

			System.out.printf("GSTT transformation complete. Created FHIR Resources: %s Patient, %s Condition and %s MedicationRequest.\n",
					patients, conditions, medications);
		}
	}

	private void addNewPatient(String patientNumber, long admitTime, String[] columns, SequenceWriter patientWriter) throws IOException {
		// Rough DOB = AdmitDTS - CurrentAge
		Date dob = new Date(admitTime - (Integer.parseInt(columns[5]) * YEAR_IN_MILLISECONDS));
		final FHIRPatient fhirPatient = new FHIRPatient(patientNumber, columns[4].equals("1") ? "male" : "female", dob);
		patientWriter.write(fhirPatient);
	}

}
