package org.snomed.heathanalytics.ingestion.bartshealth;

import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.domain.ClinicalEncounterType;
import org.snomed.heathanalytics.domain.Gender;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.ingestion.elasticsearch.ElasticOutputStream;
import org.snomed.heathanalytics.store.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

@Service
public class BartsHealthCSVLoader {

	@Autowired
	private PatientRepository patientRepository;

	private static final Pattern SCTID_PATTERN = Pattern.compile("\\d{6,18}");

	public void load(File tsvFile, ElasticOutputStream elasticOutputStream) throws IOException {
		if (!tsvFile.isFile()) {
			throw new FileNotFoundException("No file accessible at " + tsvFile.getAbsolutePath());
		}

		Long rowsLoaded = 0L;

		Map<String, AtomicLong> issues = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(tsvFile))) {
			String line;
			reader.readLine();
			while ((line = reader.readLine()) != null) {
				String[] columns = line.split(",");
				// 0=PseudoID 1=Sex 2=Birth_yr 3=Enc_Type 4=SNOMED_Cd 5=Enc_YM
				String patientId = columns[0];
				String snomedCode = columns[4];
				if (SCTID_PATTERN.matcher(snomedCode).matches()) {// Ignore rows with ICD codes
					String segmentId = snomedCode.substring(snomedCode.length() - 2, snomedCode.length() - 1);
					Long conceptId;
					if (segmentId.equals("0")) {
						conceptId = Long.parseLong(snomedCode);
					} else if (segmentId.equals("1")) {
						issues.computeIfAbsent("Row with code in SNOMED description format - please convert to concept ID.", (s) -> new AtomicLong()).incrementAndGet();
						continue;
					} else {
						issues.computeIfAbsent("Row with code which looks like SNOMED but unrecognised segmentID '" + segmentId + "'.", (s) -> new AtomicLong()).incrementAndGet();
						continue;
					}

					Patient patient = patientRepository.findOne(patientId);
					if (patient == null) {
						Date dob = yearToDate(columns[2]);
						patient = new Patient(patientId, dob, getGender(columns[1]));
						elasticOutputStream.createPatient(patient);
						System.out.println("Created Patient: " + patient);
					}
					ClinicalEncounterType type = getType(columns[3]);
					if (type == null) {
						issues.computeIfAbsent("Unrecognised type '" + columns[3] + "'", (s) -> new AtomicLong()).incrementAndGet();
					}
					ClinicalEncounter clinicalEncounter = new ClinicalEncounter(yearMonthToDate(columns[5]), type, conceptId);
					elasticOutputStream.addClinicalEncounter(patientId, clinicalEncounter);
					System.out.println("Created clinicalEncounter: " + clinicalEncounter);

					rowsLoaded++;
				} else {
//					System.out.println("code = '" + snomedCode + "'");
					issues.computeIfAbsent("Row with code which is not SNOMED format.", (s) -> new AtomicLong()).incrementAndGet();
				}
			}
		}
		if (!issues.isEmpty()) {
			System.out.println("Some issues encountered: ");
			System.out.println(issues);
			System.out.println();
		}
		System.out.println(rowsLoaded + " rows loaded.");
	}

	private Date yearMonthToDate(String column) {
		// 1900-01
		GregorianCalendar dob = new GregorianCalendar();
		dob.setTimeInMillis(0);
		dob.set(Calendar.YEAR, Integer.parseInt(column.substring(0, 4)));
		dob.set(Calendar.MONTH, Integer.parseInt(column.substring(5, 7)) - 1);
		return dob.getTime();
	}

	private ClinicalEncounterType getType(String column) {
//		problem, procedure, diagnosis
		if ("problem".equals(column)) {
			return ClinicalEncounterType.FINDING;
		} else if ("diagnosis".equals(column)) {
			return ClinicalEncounterType.FINDING;
		} else if ("procedure".equals(column)) {
			return ClinicalEncounterType.PROCEDURE;
		}
		return null;
	}

	private Date yearToDate(String column) {
		GregorianCalendar dob = new GregorianCalendar();
		dob.setTimeInMillis(0);
		dob.set(Calendar.YEAR, Integer.parseInt(column));
		return dob.getTime();
	}

	private Gender getGender(String column) {
		if ("1".equals(column)) {
			return Gender.MALE;
		} else if ("2".equals(column)) {
			return Gender.FEMALE;
		}
		return null;
	}
}
