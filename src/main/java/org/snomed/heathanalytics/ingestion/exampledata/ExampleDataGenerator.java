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

		// After 1 - 3 months
		date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 3));

		// 30 % of patients over 40 years old have hypertension.
		if (age > 40 && chancePercent(30)) {
			patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("38341003")));// Hypertension

			// 50% of patients over 40 with hypertension are prescribed an Antiplatelet agent
			if (chancePercent(50)) {
				// Prescribed an Antiplatelet agent
				patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.MEDICATION, concepts.selectRandomChildOf("108972005")));// Antiplatelet agent (product)

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// 2% of patients with hypertension who have been prescribed an Antiplatelet agent have a Myocardial Infarction.
				if (chancePercent(2)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("22298006")));// Myocardial Infarction
				}
			} else {
				// No medication prescribed

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// 8% of patients with hypertension who have NOT been prescribed an Antiplatelet agent have a Myocardial Infarction.
				if (chancePercent(8)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("22298006")));// Myocardial Infarction
				}
			}
		}

		// After 1 - 2 months
		date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 2));

		// 5% of all patients over 55 years old have Myocardial Infarction.
		if (age > 55 && chancePercent(5)) {
			patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("22298006")));// Myocardial Infarction
		}

		//
		// Begin section RA and COPD ------------------------
		//
		if (age > 15 && chancePercent(6)) {// Patients with both RA and COPD very small
			patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("69896004")));// Rheumatoid Arthritis
			patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("13645005")));// COPD

			// 50% of patients over 15 with Rheumatoid Arthritis and COPD are prescribed an Anti-TNF
			if (chancePercent(50)) {
				// Prescribed an Anti-TNF agent
				patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.MEDICATION, concepts.selectRandomChildOf("416897008")));// Anti-TNF agent (product)

				// After 1 - 6 months
				date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

				// 10% of patients with RA and COPD who have been prescribed an AntiTNF agent have a Lung Infection.
				if (chancePercent(10)) {
					patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("53084003")));// Bacterial Lung Infection
				}
			} else { // other 50%
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
		if (age > 15 && chancePercent(3)) {//Patients with RA only
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
		if (age > 15 && chancePercent(6)) {//Patients with COPD only
			patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("13645005")));// COPD

			// None with COPD alone would get anti-TNF so remove those lines

			// After 1 - 6 months
			date.add(Calendar.DAY_OF_YEAR, ThreadLocalRandom.current().nextInt(30, 30 * 6));

			// 2% of p	atients with COPD only have a Lung Infection.
			if (chancePercent(2)) {
				patient.addEncounter(new ClinicalEncounter(date.getTime(), ClinicalEncounterType.FINDING, concepts.selectRandomChildOf("53084003")));// Bacterial Lung Infection
			}
		}

		healthDataOutputStream.createPatient(patient);
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
