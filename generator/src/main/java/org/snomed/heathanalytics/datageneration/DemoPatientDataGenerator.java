package org.snomed.heathanalytics.datageneration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import org.ihtsdo.otf.sqs.service.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.model.ClinicalEncounter;
import org.snomed.heathanalytics.model.Gender;
import org.snomed.heathanalytics.model.Patient;
import org.snomed.heathanalytics.model.View;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class DemoPatientDataGenerator {

	private final SnomedConceptService concepts;

	@Autowired
	private ObjectMapper objectMapper;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public DemoPatientDataGenerator(SnomedConceptService exampleConceptService) {
		this.concepts = exampleConceptService;
	}

	public void createPatients(int patientCount, File outputFile) throws IOException {
		long start = new Date().getTime();
		List<Exception> exceptions = new ArrayList<>();
		Counters counters = new Counters();

		try (SequenceWriter patientWriter = objectMapper
				.writerFor(Patient.class)
				.withView(View.API.class)
				.withRootValueSeparator("\n")
				.writeValues(outputFile)) {

			AtomicInteger progress = new AtomicInteger();
			int progressChunk = 10_000;
			List<Patient> patientBatch = new ArrayList<>();
			for (int i = 0; i < patientCount; i++) {
				if (i % progressChunk == 0) {
					int progressToReport = progress.addAndGet(progressChunk);
					System.out.println(NumberFormat.getNumberInstance().format(progressToReport) + "/" + NumberFormat.getNumberInstance().format(patientCount));
				}
				try {
					patientBatch.add(generateExamplePatientAndActs(i + "", counters));
					if (patientBatch.size() == 1000) {
						patientWriter.writeAll(patientBatch);
						patientBatch.clear();
					}
				} catch (ServiceException | IOException e) {
					if (exceptions.size() < 10) {
						exceptions.add(e);
					}
				}
			}
			if (!patientBatch.isEmpty()) {
				try {
					patientWriter.writeAll(patientBatch);
				} catch (IOException e) {
					if (exceptions.size() < 10) {
						exceptions.add(e);
					}
				}
			}
		}
		System.out.println();
		if (!exceptions.isEmpty()) {
			logger.error("There were errors generating patent data.", exceptions.get(0));
		}
		logger.info("Generating patient data took {} seconds. All data written to NDJSON file {}.", (new Date().getTime() - start) / 1000, outputFile.getPath());

		counters.printAll();
	}

	private Patient generateExamplePatientAndActs(String roleId, Counters counters) throws ServiceException {
		Patient patient = new Patient(roleId);

		//  All patients are over the age of 30 and under the age of 85.
		int age = ThreadLocalRandom.current().nextInt(30, 85);
		patient.setDob(DateUtil.dateOfBirthFromAge(age));

		//  50% of patients are Male.
		if (chancePercent(50)) {
			patient.setGender(Gender.MALE);
		} else {
			patient.setGender(Gender.FEMALE);
		}


		// AMA scenario can create a couple of years of history but uses it's own date object
		scenarioAMA(patient, age, counters);

		// Start 3 years ago
		GregorianCalendar healthRecordDate = new GregorianCalendar();
		healthRecordDate.add(Calendar.YEAR, -3);

		// Add 1 - 6 months
		healthRecordDate.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

		// Can also move record date forward
		generateBackgroundData(patient, healthRecordDate);

		// Add 1 - 3 months
		healthRecordDate.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 3));

		// All patients enter each scenario. These have their own probability gates.
		GregorianCalendar today = new GregorianCalendar();
		if (healthRecordDate.before(today)) {
			scenarioRaCOPD(patient, age, healthRecordDate);
			scenarioPulmEmbGIBleed(patient, age, healthRecordDate);
			scenarioBrcaTamoxifen(patient, age, healthRecordDate);
			scenarioDiabSmokeFootAmp(patient, age, healthRecordDate);
			scenarioLymphAnthCHF(patient, age, healthRecordDate);
			scenarioCOVID19(patient, age, healthRecordDate);
		}
		return patient;
	}

	private void scenarioAMA(Patient patient, int age, Counters counters) throws ServiceException {
		// Scenario for 2 years of history
		GregorianCalendar date = new GregorianCalendar();
		date.add(Calendar.YEAR, -2);
		date.add(Calendar.MONTH, -3);

		if (chancePercent(patient.getGender() == Gender.FEMALE && age >= 60 && age < 79, 9.77f, "ama.all")) {
			Calendar today = new GregorianCalendar();

			float screensInTwoYears, chanceOfAbnormalScreening, chanceOfAbnormalDiagnostic, chanceOfPositiveBiopsy, chanceOfStage1, chanceOfStage2, baseScreeningGap;
			String route;

			// 53% of total are screened Annually
			if (chancePercent(53f)) {
				route = "ama.annual";
				chanceOfAbnormalScreening = 6.5f;
				chanceOfAbnormalDiagnostic = 49.2f;
				chanceOfPositiveBiopsy = 17;
				chanceOfStage1 = 42;
				chanceOfStage2 = 36;
				baseScreeningGap = 365;
			} else if (chancePercentOfRemaining(27f, 100 - 53)) {
				route = "ama.biennial";
				chanceOfAbnormalScreening = 6.5f;
				chanceOfAbnormalDiagnostic = 36.9f;
				chanceOfPositiveBiopsy = 22;
				chanceOfStage1 = 39;
				chanceOfStage2 = 42;
				baseScreeningGap = 365 * 2;
			} else {
				return;
			}

			counters.inc(route);

			// Repeat screening until today
			int screensComplete = 0;
			while (date.before(today)) {
				// 384151000119104 | Screening mammography of bilateral breasts (procedure) |
				patient.addEncounter(new ClinicalEncounter(date.getTime(), 384151000119104L));

				// Selection requires at least two screens so don't allow abnormal screening on first round.
				if (screensComplete > 0 && chancePercent(chanceOfAbnormalScreening, route + ".abnormalScreen")) {
					counters.inc(route + ".abnormalScreen");

					// Abnormal screening:
					// 171176006 | Breast neoplasm screening abnormal (finding) |
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("171176006")));

					// .. leads to diagnostic:
					// 566571000119105 | Mammography of right breast (procedure) |
					date.add(Calendar.DAY_OF_YEAR, 5);
					patient.addEncounter(new ClinicalEncounter(date.getTime(), 566571000119105L));

					if (chancePercent(chanceOfAbnormalDiagnostic, route + ".abnormalDiagnostic")) {
						counters.inc(route + ".abnormalDiagnostic");

						// Abnormal diagnostic:
						// 274530001 | Abnormal findings on diagnostic imaging of breast (finding) |
						patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("274530001")));

						// .. leads to biopsy
						// 122548005 | Biopsy of breast (procedure) |
						patient.addEncounter(new ClinicalEncounter(date.getTime(), 122548005L));

						if (chancePercent(chanceOfPositiveBiopsy, route + ".positiveBiopsy")) {
							counters.inc(route + ".positiveBiopsy");

							// Positive biopsy
							// 165325009 | Biopsy result abnormal (finding) |
							patient.addEncounter(new ClinicalEncounter(date.getTime(), 165325009L));

							if (chancePercent(chanceOfStage1, route + ".stage1")) {
								// Stage 1
								// 422399001 | Infiltrating ductal carcinoma of breast, stage 1 (finding) |
								patient.addEncounter(new ClinicalEncounter(date.getTime(), 422399001L));
								counters.inc(route + ".stage1");
							} else {
								if (chancePercentOfRemaining(chanceOfStage2, 100 - chanceOfStage1, route + ".stage2")) {
									// Stage 2
									// 422479008 | Infiltrating ductal carcinoma of breast, stage 2 (finding) |
									patient.addEncounter(new ClinicalEncounter(date.getTime(), 422479008L));
									counters.inc(route + ".stage2");
								} else {
									// DCIS
									// 397201007 | Microcalcifications present in ductal carcinoma in situ (finding) |
									patient.addEncounter(new ClinicalEncounter(date.getTime(), 397201007L));
									counters.inc(route + ".DCIS");
								}
							}
						}
					}
//					break;
				}

				// Move record date on by screening gap +/- 0-30 days.
				date.add(Calendar.DAY_OF_YEAR, (int) Math.round(baseScreeningGap + ((Math.random() * 60) - 30)));
				screensComplete++;
			}
		}
	}

	private void scenarioRaCOPD(Patient patient, int age, GregorianCalendar date) throws ServiceException {
		//
		// Begin section RA and COPD ------------------------
		//
		if (age > 15 && chancePercent(0.12f)) {// Patients with both RA and COPD very small
			patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("69896004")));// Rheumatoid Arthritis
			patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("13645005")));// COPD

			// 10% of patients over 15 with Rheumatoid Arthritis and COPD are prescribed an Anti-TNF
			if (chancePercent(10)) {
				// Prescribed an Anti-TNF agent
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("416897008")));// Anti-TNF agent (product)

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// 10% of patients with RA and COPD who have been prescribed an AntiTNF agent have a Lung Infection.
				if (chancePercent(10)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("53084003")));// Bacterial pneumonia
				}
			} else { // other 90%
				// No medication prescribed

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// 2% of patients with RA and COPD who have NOT been prescribed an Anti-TNF agent have a Bacterial pneumonia
				if (chancePercent(2)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("53084003")));// Bacterial pneumonia
				}
			}
		}
		// End of section of RA and COPD ---------------------

		// Begin section RA only  ----------------------------
		if (age > 15 && chancePercent(6)) {//Patients with RA only
			patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("69896004")));// Rheumatoid Arthritis

			if (chancePercent(50)) {// about half of them get TNF inhibitor
				// Prescribed an Anti-TNF agent
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("416897008")));// Anti-TNF agent (product)


				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// 4% of patients with RA only who have been prescribed an AntiTNF agent have a Lung Infection.
				if (chancePercent(4)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("53084003")));// Bacterial Lung Infection
				}
			} else {
				// No medication prescribed

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// 0.5% of patients with who have NOT been prescribed an Anti-TNF agent have a Bacterial Lung Infection.
				if (chancePercent(0.5f)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("53084003")));// Bacterial Lung Infection
				}
			}
		}
		// End of section of RA only ----------------------

		// Begin section of COPD only ---------------------
		// 6% with COPD
		if (age > 15 && chancePercent(12)) {//Patients with COPD only
			patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("13645005")));// COPD

			// None with COPD alone would get anti-TNF so remove those lines

			// After 1 - 6 months
			date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

			// 2% of patients with COPD only have a Lung Infection.
			if (chancePercent(2)) {
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("53084003")));// Bacterial Lung Infection
			}
		}
	}

	// This scenario is not used at the moment.

	@SuppressWarnings("unused")
	private void scenarioAfibPepticUcler(Patient patient, int age, GregorianCalendar date) throws ServiceException {
		//
		// Begin section Afib and Peptic Ucler ------------------------
		//
		if (age > 15 && chancePercent(0.015f)) {// Patients with both Afib and Ulcer very small
			patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("49436004")));// Afib
			patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("13200003")));// Peptic Ulcer

			// 25% of patients over 15 with Afib and Ulcer are prescribed an Antiplatelet agent
			if (chancePercent(25)) {
				// Prescribed an AntiPlatelet
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("108972005")));// Antiplatelet Agent

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// 4% of patients with Afib and Ulcer who have been prescribed an Antiplatelt agent have a CVA.
				if (chancePercent(4)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("230690007")));// CVA
				}
				//  And 14% get subsequent UGIB
				if (chancePercent(14))
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("37372002")));// UGIB
			} else { // other 85%
				// No medication prescribed

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// 12% of patients with Afib and UGIB and no Antiplatelet agent get CVA
				if (chancePercent(12))
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("230690007")));// CVA
				// and 8% get UGIB
				if (chancePercent(8))
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("37372002")));// UGIB

			}
		}

		// End of section of Afib and Ucler ---------------------

		// Begin section Afib only  ----------------------------
		if (age > 15 && chancePercent(1)) {//Patients with Afib only
			patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("49436004")));// Afib

			if (chancePercent(89)) {// Get antiplatelet agent
				// Prescribed an Antiplatelet
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("108972005")));// Antiplatelet agent (product)


				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// 4% of patients with Afib only who have been prescribed an AntiTNF agent have a CVA.
				if (chancePercent(4)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("230690007")));// CVA
				}
				if (chancePercent(1)) {  // get UGIB
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("37372002")));// UGIB
				}
			} else {
				// No medication prescribed

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// 12% get CVA.
				if (chancePercent(12)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("230690007")));// CVA
				}
			}
		}
		// End of section of Afib only ----------------------

		// Begin section of Ulcer only ---------------------
		// 9% with Ulcer
		if (age > 15 && chancePercent(9)) {//Patients with Ulcer only
			patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("13200003")));// Ulcer

			// None with Ulcer alone would get antiplatelet so remove those lines

			// After 1 - 6 months
			date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

			// 8% of patients with Ulcer only will get recurrent UGIB.
			if (chancePercent(8)) {
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("37372002")));// UGIB
			}
		}
	}
	private void scenarioPulmEmbGIBleed(Patient patient, int age, GregorianCalendar date) throws ServiceException {
		//
		// Begin section Pulm Embolus and GI Ulcer ------------------------
		// Disease codes for this are GI Ulcer 40845000, GI Bleed 74474003, Pulmonary thromboembolism 233935004, and Product containing warfarin 48603004
		if (age > 15 && chancePercent(0.15f)) {// Patients with both Pulm Embolous and Ulcer very small
			patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("233935004")));// Pulmonary thromboembolism (disorder)
			patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("40845000")));// GI ULcer Disease

			// 75% of patients over 15 with Pulm Emb and Ulcer are prescribed an AntiCoagulant agent
			if (chancePercent(75)) {
				// Prescribed an AntiCoag Agent
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("48603004")));// Product containing warfarin (medicinal product)

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));
				// 25% get GI bleed
				if (chancePercent(25)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("74474003")));// GI Bleed
				}

			} else { // other 25%
				// No medication prescribed

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// and 4% get GIB
				if (chancePercent(4)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("74474003")));// GIB
				}
			}
		}

		// End of section of pulm emb and GI ulcer ---------------------

		// Begin section Pulm Emb only  ----------------------------
		if (age > 15 && chancePercent(2)) {// Patients with Pulm Emb only
			patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("233935004")));// Pulmonary thromboembolism (disorder)

			if (chancePercent(92)) {// Get AntiCoag agent
				// Prescribed an Antiplatelet
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("48603004")));// Product containing warfarin (medicinal product)


				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// 0.5% get GI Bleed
				if (chancePercent(0.5f)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("74474003")));// GIB
				}
			} else {
				// No medication prescribed

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// 0.01% get GIB
				if (chancePercent(0.01f)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("74474003")));// GIB
				}
			}
		}
		// End of section of Pulm Emb only ----------------------

		// Begin section of Ulcer only ---------------------
		// 4% with Ulcer
		if (age > 15 && chancePercent(4)) {// Patients with Ulcer only
			patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("40845000")));// GI Ulcer

			// None with Ulcer alone would get antiCoag so those lines are removed

			// After 1 - 6 months
			date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

			// 1% of patients with Ulcer only will get recurrent GIB.
			if (chancePercent(1)) {
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("74474003")));// GIB
			}
		}
	}

	private void scenarioBrcaTamoxifen(Patient patient, int age, GregorianCalendar date) throws ServiceException {
		if (age > 30 && patient.getGender() == Gender.FEMALE && chancePercent(0.2f)) {// females with brca1 gene
			patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("412734009")));// 412734009 |BRCA1 gene mutation positive (finding)|

			// Percent that get Tamoxifen
			if (chancePercent(60)) {
				// got product or procedure  types include FINDING, MEDICATION, PROCEDURE
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("75959001")));// 75959001 |Product containing tamoxifen (medicinal product)|
				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// Percent with Breast Cancer
				if (chancePercent(29)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("254837009")));// 254837009 |Malignant neoplasm of breast (disorder)|
				}
			} else {// did not get drug or procedure

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// did not get tamoxifen
				if (chancePercent(72)){
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("254837009")));// 254837009 |Malignant neoplasm of breast (disorder)|
				}
			}
		}
	}

	private void scenarioDiabSmokeFootAmp(Patient patient, int age, GregorianCalendar date) throws ServiceException {
		if (age > 30 && chancePercent(2)) {
			patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("44054006")));// 44054006 |Diabetes mellitus type 2 (disorder)|

			if (chancePercent(5)) {
				// ### got product or procedure types include FINDING, MEDICATION, PROCEDURE
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("77176002")));// 77176002 |Smoker (finding)|

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// ### Percent with both diseases that got the outcome
				if (chancePercent(1.5f)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("180030006")));// 180030006 |Amputation of the foot (procedure)|
				}
			} else { // not smoker
				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				if (chancePercent(0.5f)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("180030006")));// 180030006 |Amputation of the foot (procedure)|
				}
			}
		}
	}

	private void scenarioLymphAnthCHF(Patient patient, int age, GregorianCalendar date) throws ServiceException {
		if (age > 30 && chancePercent(0.1f)) {// Both Diseases
			patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("109979007")));// 109979007 |B-cell lymphoma (disorder)|
			patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("57809008")));// 57809008 |Myocardial disease (disorder)|


			// Percent that get the product or procedure
			if (chancePercent(20)) {
				// got product or procedure  types include FINDING, MEDICATION, PROCEDURE
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("108787006")));// 108787006 |Product containing anthracycline (product)|

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// Percent with both diseases that got the outcome
				if (chancePercent(20)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("42343007")));// 42343007 |Congestive heart failure (disorder)|
				}
			} else { // did not get drug or procedure


				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));


				if (chancePercent(5)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("42343007")));// 42343007 |Congestive heart failure (disorder)|
				}
			}
		}
		// End of section of both diseases section ---------------------

		// Begin section First disease only  ----------------------------
		if (age > 15 && chancePercent(2)) {
			patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("109979007")));// 109979007 |B-cell lymphoma (disorder)|

			if (chancePercent(20)) {// ### who got antracycline

				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("108787006")));// 108787006 |Product containing anthracycline (product)|

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// # first disease and gets product and complication
				if (chancePercent(3)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("42343007")));// 42343007 |Congestive heart failure (disorder)|
				}
			} else {
				// No product or procedure

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// first disease no product and gets complication
				if (chancePercent(0.3f)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("42343007")));// 42343007 |Congestive heart failure (disorder)|
				}
			}
		}
		// End of section of disease one only----------------------

		// Begin section of disease two only ---------------------
		// percent with disease two
		if (age > 15 && chancePercent(3)) {
			patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("57809008")));// 57809008 |Myocardial disease (disorder)|

			// none with only disease two get product.

			// After 1 - 6 months
			date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

			// get complication naturally
			if (chancePercent(5)) {
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("42343007")));// 42343007 |Congestive heart failure (disorder)|
			}
		}
	}

	private void generateBackgroundData(Patient patient, GregorianCalendar date) throws ServiceException {
		// 10% of patients have diabetes.
		if (chancePercent(10)) {
			patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("420868002")));// Disorder due to type 1 diabetes mellitus

			// After 1 - 2 months
			date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 2));

			// 7% of the diabetic patients also have Peripheral Neuropathy.
			if (chancePercent(7)) {
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("302226006")));// Peripheral Neuropathy
			}

			// 10% of the diabetic patients have a Myocardial Infarction.
			if (chancePercent(10)) {
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("22298006")));// Myocardial Infarction
			}
		} else {
			// 1% of the non-diabetic patients have Peripheral Neuropathy.
			if (chancePercent(1)) {
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("302226006")));// Peripheral Neuropathy
			}
		}
	}

	private void scenarioCOVID19(Patient patient, int age, GregorianCalendar date) throws ServiceException {
		// Exclusively assign patient to one of 10 sub scenarios
		int group = new Random().nextInt(10);

		// 40% of all patients get into scenario
		if(age > 15 && chancePercent(40.0f)) {

			// A
			if (group == 0) {
				// covid detected, nothing happens
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("1240581000000104")));// covid detected
			}

			// B
			if (group == 1) {
				// covid detected, get disease
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("1240581000000104")));// covid detected
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("840539006")));// covid disease

				if (chancePercent(2.0f)) {
					// covid pneumonia
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("882784691000119100")));// COVID-19 pneumonia
					if (chancePercent(10.0f)) {
						// dead
						patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("419099009")));// 419099009 | Dead (finding) |
					}
				}
			}

			// C
			if (group == 2){
				// Obesity
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("414916001")));// 414916001 | Obesity (disorder) |

				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("1240581000000104")));// covid detected
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("840539006")));// covid disease

				if (chancePercent(20.0f)) {
					// covid pneumonia
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("882784691000119100")));// COVID-19 pneumonia
					if(chancePercent(20.0f)) {
						// dead
						patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("419099009")));// 419099009 | Dead (finding) |
					}
				}
			}

			// D
			if (group == 3) {
				// Diabetes
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("73211009")));// 73211009 | Diabetes mellitus (disorder) |

				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("1240581000000104")));// covid detected
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("840539006")));// covid disease

				if(chancePercent(25.0f)) {
					// covid pneumonia
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("882784691000119100")));// COVID-19 pneumonia
					if(chancePercent(25.0f)) {
						// dead
						patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("419099009")));// 419099009 | Dead (finding) |
					}
				}
			}

			// E
			if (group == 4) {
				// Hypertension // TODO what happens to this group?
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("38341003")));// 38341003 | Hypertensive disorder, systemic arterial (disorder) |

				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("1240581000000104")));// covid detected
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("840539006")));// covid disease
			}

			// F
			if (group == 5) {
				// covid pneumonia // TODO similar to group B?
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("882784691000119100")));// COVID-19 pneumonia
				if (chancePercent(18.0f)) {
					// dead
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("419099009")));// 419099009 | Dead (finding) |
				}
			}

			// G
			if (group == 6){
				// Obesity and Diabetes
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("414916001")));// 414916001 | Obesity (disorder) |
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("73211009")));// 73211009 | Diabetes mellitus (disorder) |

				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("1240581000000104")));// covid detected
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("840539006")));// covid disease

				if (chancePercent(32.0f)) {
					// covid pneumonia
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("882784691000119100")));// COVID-19 pneumonia
					if (chancePercent(32.0f)) {
						// dead
						patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("419099009")));// 419099009 | Dead (finding) |
					}
				}
			}

			// H
			if (group == 7) {
				// Obesity and Hypertension
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("414916001")));// 414916001 | Obesity (disorder) |
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("38341003")));// 38341003 | Hypertensive disorder, systemic arterial (disorder) |

				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("1240581000000104")));// covid detected
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("840539006")));// covid disease

				if (chancePercent(28.0f)) {
					// covid pneumonia
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("882784691000119100")));// COVID-19 pneumonia
					if (chancePercent(28.0f)) {
						// dead
						patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("419099009")));// 419099009 | Dead (finding) |
					}
				}
			}

			// I
			if (group == 8) {
				// Diabetes and Hypertension
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("73211009")));// 73211009 | Diabetes mellitus (disorder) |
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("38341003")));// 38341003 | Hypertensive disorder, systemic arterial (disorder) |

				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("1240581000000104")));// covid detected
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("840539006")));// covid disease

				if (chancePercent(25.0f)) {
					// covid pneumonia
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("882784691000119100")));// COVID-19 pneumonia
					if (chancePercent(25.0f)) {
						// dead
						patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("419099009")));// 419099009 | Dead (finding) |
					}
				}
			}

			// J
			if (group == 9) {
				// Obesity, Diabetes and Hypertension
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("414916001")));// 414916001 | Obesity (disorder) |
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("73211009")));// 73211009 | Diabetes mellitus (disorder) |
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("38341003")));// 38341003 | Hypertensive disorder, systemic arterial (disorder) |

				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("1240581000000104")));// covid detected
				patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("840539006")));// covid disease

				if (chancePercent(48.0f)) {
					// covid pneumonia
					patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("882784691000119100")));// COVID-19 pneumonia
					if (chancePercent(48.0f)) {
						// dead
						patient.addEncounter(new ClinicalEncounter(date.getTime(), concepts.selectRandomChildOf("419099009")));// 419099009 | Dead (finding) |
					}
				}
			}
		}
	}

	private boolean chancePercentOfRemaining(float probabilityPercent, float ofRemainingPercentage) {
		return chancePercentOfRemaining(probabilityPercent, ofRemainingPercentage, null);
	}

	private boolean chancePercentOfRemaining(float probabilityPercent, float ofRemainingPercentage, String gateId) {
		// e.g. 25% chance if only 75% remaining = 33.3% chance
		return chancePercent(probabilityPercent * (100f / ofRemainingPercentage), gateId);
	}

	private boolean chancePercent(float probabilityPercentage) {
		return chance(true, probabilityPercentage / 100, null);
	}

	private boolean chancePercent(float probabilityPercentage, String gateId) {
		return chance(true, probabilityPercentage / 100, gateId);
	}

	private boolean chancePercent(boolean preCondition, float probabilityPercentage, String gateId) {
		return chance(preCondition, probabilityPercentage / 100, gateId);
	}

	private final Map<String, Pair<AtomicInteger, AtomicInteger>> chanceResultsSoFar = new HashMap<>();

	private boolean chance(boolean preCondition, float probabilityFraction, String gateId) {
		AtomicInteger hits = null;
		if (gateId != null) {
			Pair<AtomicInteger, AtomicInteger> totalAndHitPair = chanceResultsSoFar.computeIfAbsent(gateId, (id) -> Pair.of(new AtomicInteger(), new AtomicInteger()));
			AtomicInteger total = totalAndHitPair.getFirst();
			hits = totalAndHitPair.getSecond();
			if (total.get() > 5) {
				int desiredResult = Math.round(total.get() * probabilityFraction);
				float diffAllowance = 0.05f;
				if (hits.get() > desiredResult) {
					probabilityFraction = probabilityFraction * 0.1f;
					if (hits.get() >= desiredResult * (1 + diffAllowance)) {
						probabilityFraction = 0f;
					}
				} else if (hits.get() < desiredResult) {
					probabilityFraction = probabilityFraction * 10f;
					if (hits.get() < desiredResult * (1 - diffAllowance)) {
						probabilityFraction = 1f;
					}
				}
			}
			total.incrementAndGet();
		}
		if (preCondition && probabilityFraction >= Math.random()) {
			if (hits != null) {
				hits.incrementAndGet();
			}
			return true;
		} else {
			return false;
		}
	}

}
