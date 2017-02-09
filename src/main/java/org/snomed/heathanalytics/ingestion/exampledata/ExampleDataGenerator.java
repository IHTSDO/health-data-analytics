package org.snomed.heathanalytics.ingestion.exampledata;

import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.domain.Sex;
import org.snomed.heathanalytics.ingestion.HealthDataIngestionSource;
import org.snomed.heathanalytics.ingestion.HealthDataOutputStream;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static org.snomed.heathanalytics.ingestion.exampledata.ExampleDataGenerator.DateUtil.dateOfBirthFromAge;
import static org.snomed.heathanalytics.ingestion.exampledata.ExampleDataGenerator.DateUtil.getAge;

public class ExampleDataGenerator implements HealthDataIngestionSource {

	private final ExampleConceptService concepts;
	private int numberOfPatients;

	public ExampleDataGenerator(ExampleConceptService exampleConceptService, int numberOfPatients) {
		this.concepts = exampleConceptService;
		this.numberOfPatients = numberOfPatients;
	}

	@Override
	public void stream(HealthDataOutputStream healthDataOutputStream) {
		IntStream.range(0, numberOfPatients).parallel().forEach(i -> generateExamplePatientAndActs(healthDataOutputStream));
	}

	private void generateExamplePatientAndActs(HealthDataOutputStream healthDataOutputStream) {
		Patient patient = new Patient();

		//  All patients are over the age of 30 and under the age of 85.
		patient.setDob(dateOfBirthFromAge(ThreadLocalRandom.current().nextInt(30, 85)));

		//  50% of patients are Male.
		if (chance(0.5f)) {
			patient.setSex(Sex.MALE);
		} else {
			patient.setSex(Sex.FEMALE);
		}

		String roleId = healthDataOutputStream.createPatient(patient.getName(), patient.getDob(), patient.getSex());

		// 10% of patients have diabetes.
		if (chance(0.1f)) {
			healthDataOutputStream.addCondition(roleId, concepts.selectChildOf("420868002 | Disorder due to type 1 diabetes mellitus"));

			// 7% of the diabetic patients also have Peripheral Neuropathy.
			if (chance(0.07f)) {
				healthDataOutputStream.addCondition(roleId, concepts.selectChildOf("?? | Peripheral Neuropathy"));
			}

			// 10% of the diabetic patients have a Myocardial Infarction.
			if (chance(0.1f)) {
				healthDataOutputStream.addCondition(roleId, concepts.selectChildOf("?? | Myocardial Infarction"));
			}
		} else {
			// 1% of the non-diabetic patients have Peripheral Neuropathy.
			if (chance(0.01f)) {
				healthDataOutputStream.addCondition(roleId, concepts.selectChildOf("?? | Peripheral Neuropathy"));
			}
		}

		// 30 % of patients over 40 years old have hypertension.
		if (getAge(patient.getDob()) > 40 && chance(0.3f)) {
			healthDataOutputStream.addCondition(roleId, concepts.selectChildOf("?? | Hypertension"));

			// 8% of patients with hypertension have a Myocardial Infarction.
			if (chance(0.08f)) {
				healthDataOutputStream.addCondition(roleId, concepts.selectChildOf("?? | Myocardial Infarction"));
			}
		}

		// 5% of all patients over 55 years old have Myocardial Infarction.
		if (getAge(patient.getDob()) > 55 && chance(0.05f)) {
			healthDataOutputStream.addCondition(roleId, concepts.selectChildOf("?? | Myocardial Infarction"));
		}
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

		static int getAge(Date patientDob) {
			// Rough calculation for example data
			return Math.round((new Date().getTime() - patientDob.getTime()) / millisecondsInAYear);
		}

		static Date dateOfBirthFromAge(int ageInYears) {
			GregorianCalendar date = new GregorianCalendar();
			date.add(Calendar.YEAR, -ageInYears);
			return date.getTime();
		}
	}
}
