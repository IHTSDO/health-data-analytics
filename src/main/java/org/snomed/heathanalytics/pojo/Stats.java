package org.snomed.heathanalytics.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class Stats {

	private Date date;
	private final long patientCount;

	public Stats(Date date, long patientCount) {
		this.date = date;
		this.patientCount = patientCount;
	}

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
	public Date getDate() {
		return date;
	}

	public long getPatientCount() {
		return patientCount;
	}

}
