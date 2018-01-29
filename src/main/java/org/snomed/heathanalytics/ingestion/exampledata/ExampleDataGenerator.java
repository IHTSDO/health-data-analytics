package org.snomed.heathanalytics.ingestion.exampledata;

import org.ihtsdo.otf.sqs.service.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.domain.ClinicalEncounterType;
import org.snomed.heathanalytics.domain.Gender;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.ingestion.HealthDataIngestionSource;
import org.snomed.heathanalytics.ingestion.HealthDataIngestionSourceConfiguration;
import org.snomed.heathanalytics.ingestion.HealthDataOutputStream;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.snomed.heathanalytics.ingestion.exampledata.ExampleDataGenerator.DateUtil.dateOfBirthFromAge;

public class ExampleDataGenerator implements HealthDataIngestionSource {

	private final ExampleConceptService concepts;
	private Logger logger = LoggerFactory.getLogger(getClass());

	public ExampleDataGenerator(ExampleConceptService exampleConceptService) {
		this.concepts = exampleConceptService;
	}

	@Override
	public void stream(HealthDataIngestionSourceConfiguration configuration, HealthDataOutputStream healthDataOutputStream) {
		ExampleDataGeneratorConfiguration generatorConfiguration = (ExampleDataGeneratorConfiguration) configuration;
		long start = new Date().getTime();
		List<Exception> exceptions = new ArrayList<>();
		AtomicInteger progress = new AtomicInteger();
		int progressChunk = 10_000;
		IntStream.range(0, generatorConfiguration.getDemoPatientCount()).parallel().forEach(i -> {
			if (i % progressChunk == 0) {
				int progressToReport = progress.addAndGet(progressChunk);
				System.out.println(NumberFormat.getNumberInstance().format(progressToReport) + "/" + NumberFormat.getNumberInstance().format(generatorConfiguration.getDemoPatientCount()));
			}
			try {
				generateExamplePatientAndActs(i + "", healthDataOutputStream);
			} catch (ServiceException e) {
				if (exceptions.size() < 10) {
					exceptions.add(e);
				}
			}
		});
		System.out.println();
		if (!exceptions.isEmpty()) {
			logger.error("There were errors generating patent data.", exceptions.get(0));
		}
		logger.info("Generating patient data took {} seconds.", (new Date().getTime() - start) / 1000);
	}

	private void generateExamplePatientAndActs(String roleId, HealthDataOutputStream healthDataOutputStream) throws ServiceException {
		Patient patient = new Patient(roleId);

		//  All patients are over the age of 30 and under the age of 85.
		int age = ThreadLocalRandom.current().nextInt(30, 85);
		patient.setDob(dateOfBirthFromAge(age));

		//  50% of patients are Male.
		if (chancePercent(50)) {
			patient.setGender(Gender.MALE);
		} else {
			patient.setGender(Gender.FEMALE);
		}

		// Start 2 years ago
		GregorianCalendar date = new GregorianCalendar();
		date.add(Calendar.YEAR, -2);

		// add 1 - 6 months
		date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

		generateBackgroundData(patient, date);

		// After 1 - 3 months
		date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 3));

		if (chancePercent(50)) {
			// 50% of total
			scenarioRaCOPD(patient, age, date);
		} else {
			// 50% of total
			scenarioAfibGIBleed(patient, age, date);
		}

		healthDataOutputStream.createPatient(patient);
	}

	private void scenarioRaCOPD(Patient patient, int age, GregorianCalendar date) throws ServiceException {
		//
		// Begin section RA and COPD ------------------------
		//
		if (age > 15 && chancePercent(12)) {// Patients with both RA and COPD very small
			patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("69896004")));// Rheumatoid Arthritis
			patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("13645005")));// COPD

			// 10% of patients over 15 with Rheumatoid Arthritis and COPD are prescribed an Anti-TNF
			if (chancePercent(10)) {
				// Prescribed an Anti-TNF agent
				patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.MEDICATION, concepts.selectRandomChildOf("416897008")));// Anti-TNF agent (product)

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// 10% of patients with RA and COPD who have been prescribed an AntiTNF agent have a Lung Infection.
				if (chancePercent(10)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("53084003")));// Bacterial Lung Infection
				}
			} else { // other 90%
				// No medication prescribed

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// 2% of patients with RA and COPD who have NOT been prescribed an Anti-TNF agent have a Bacterial Lung Infection.
				if (chancePercent(2)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("53084003")));// Bacterial Lung Infection
				}
			}
		}
		// End of section of RA and COPD ---------------------

		// Begin section RA only  ----------------------------
		if (age > 15 && chancePercent(6)) {//Patients with RA only
			patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("69896004")));// Rheumatoid Arthritis

			if (chancePercent(50)) {// about half of them get TNF inhibitor
				// Prescribed an Anti-TNF agent
				patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.MEDICATION, concepts.selectRandomChildOf("416897008")));// Anti-TNF agent (product)


				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// 4% of patients with RA only who have been prescribed an AntiTNF agent have a Lung Infection.
				if (chancePercent(4)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("53084003")));// Bacterial Lung Infection
				}
			} else {
				// No medication prescribed

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// 0.5% of patients with who have NOT been prescribed an Anti-TNF agent have a Bacterial Lung Infection.
				if (chancePercent(0.5f)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("53084003")));// Bacterial Lung Infection
				}
			}
		}
		// End of section of RA only ----------------------

		// Begin section of COPD only ---------------------
		// 6% with COPD
		if (age > 15 && chancePercent(12)) {//Patients with COPD only
			patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("13645005")));// COPD

			// None with COPD alone would get anti-TNF so remove those lines

			// After 1 - 6 months
			date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

			// 2% of p	atients with COPD only have a Lung Infection.
			if (chancePercent(2)) {
				patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("53084003")));// Bacterial Lung Infection
			}
		}
	}

	private void scenarioAfibGIBleed(Patient patient, int age, GregorianCalendar date) throws ServiceException {
		//
		// Begin section Afib and GI Bleed (GIB)------------------------
		//
		if (age > 15 && chancePercent(0.015f)) {// Patients with both Afib and GIB very small
			patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("49436004")));// Afib
			patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("74474003")));// GIB

			// 15% of patients over 15 with Afib and GIB are prescribed an Antiplatelet agent
			if (chancePercent(15)) {
				// Prescribed an AntiPlatelet
				patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.MEDICATION, concepts.selectRandomChildOf("108972005")));// Antiplatelet Agent

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// 4% of patients with Afib and GIB who have been prescribed an Antiplatelt agent have a CVA.
				if (chancePercent(4)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("230690007")));// CVA
				}
				//  And 32% get subsequent GIB
				if (chancePercent(32))
					patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("74474003")));// GIB
			} else { // other 85%
				// No medication prescribed

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// 35% of patients with Afib and GIB and no Antiplatelet agent get CVA
				if (chancePercent(35))
					patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("230690007")));// CVA
				// and 9% get GIB
				if (chancePercent(9))
					patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("74474003")));// GIB

			}
		}

		// End of section of Afib and GIB ---------------------

		// Begin section Afib only  ----------------------------
		if (age > 15 && chancePercent(1)) {//Patients with Afib only
			patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("49436004")));// Afib

			if (chancePercent(89)) {// Get antiplatelet agent
				// Prescribed an Antiplatelet
				patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.MEDICATION, concepts.selectRandomChildOf("108972005")));// Antiplatelet agent (product)


				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// 3% of patients with Afib only who have been prescribed an AntiTNF agent have a CVA.
				if (chancePercent(3)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("230690007")));// CVA
				}
				if (chancePercent(0.1f)) {  // get GIB
					patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("74474003")));// GIB
				}
			} else {
				// No medication prescribed

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// 31% get CVA.
				if (chancePercent(31)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("230690007")));// CVA
				}
			}
		}
		// End of section of Afib only ----------------------

		// Begin section of GIB only ---------------------
		// 0.13% with COPD
		if (age > 15 && chancePercent(13f)) {//Patients with GIB only
			patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("74474003")));// GIB

			// None with GIB alone would get antiplatelet so remove those lines

			// After 1 - 6 months
			date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

			// 9% of p atients with GIB only will get recurrent GIB.
			if (chancePercent(9)) {
				patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("74474003")));// GIB
			}
		}
	}

	private void generateBackgroundData(Patient patient, GregorianCalendar date) throws ServiceException {
		// 10% of patients have diabetes.
		if (chancePercent(10)) {
			patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("420868002")));// Disorder due to type 1 diabetes mellitus

			// After 1 - 2 months
			date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 2));

			// 7% of the diabetic patients also have Peripheral Neuropathy.
			if (chancePercent(7)) {
				patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("302226006")));// Peripheral Neuropathy
			}

			// 10% of the diabetic patients have a Myocardial Infarction.
			if (chancePercent(10)) {
				patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("22298006")));// Myocardial Infarction
			}
		} else {
			// 1% of the non-diabetic patients have Peripheral Neuropathy.
			if (chancePercent(1)) {
				patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("302226006")));// Peripheral Neuropathy
			}
		}
	}

	private boolean chancePercent(float probability) {
		return chance(probability / 100);
	}

	private boolean chance(float probability) {
		return probability >= Math.random();
	}

	static final class DateUtil {

		static long millisecondsInAYear;
		static {
			GregorianCalendar calendar = new GregorianCalendar();
			calendar.setTime(new Date(0));
			calendar.add(Calendar.YEAR, 1);
			millisecondsInAYear = calendar.getTime().getTime();
		}

		static Date dateOfBirthFromAge(int ageInYears) {
			GregorianCalendar date = new GregorianCalendar();
			date.add(Calendar.YEAR, -ageInYears);
			clearTime(date);
			return date.getTime();
		}

		static GregorianCalendar clearTime(GregorianCalendar calendar) {
			calendar.clear(Calendar.HOUR);
			calendar.clear(Calendar.MINUTE);
			calendar.clear(Calendar.SECOND);
			calendar.clear(Calendar.MILLISECOND);
			return calendar;
		}
	}
}
