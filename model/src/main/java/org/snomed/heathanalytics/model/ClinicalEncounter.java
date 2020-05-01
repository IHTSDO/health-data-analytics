package org.snomed.heathanalytics.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.snomed.heathanalytics.model.pojo.TermHolder;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Calendar;
import java.util.Date;

public class ClinicalEncounter {

	@Field
	private Date date;

	@Field(type = FieldType.Keyword)
	private Long conceptId;

	@Transient
	private TermHolder conceptTerm;

	public interface Fields {
		String ROLE_ID = "roleId";
		String DATE = "date";
		String CONCEPT_ID = "conceptId";
	}

	public ClinicalEncounter() {
	}

	public ClinicalEncounter(Date date, Long conceptId) {
		this.date = date;
		this.conceptId = conceptId;
	}

	public ClinicalEncounter(Calendar date, Long conceptId) {
		this(date.getTime(), conceptId);
	}

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
	public Date getDate() {
		return date;
	}

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMddhhmmss")
	@JsonProperty(defaultValue = "yyyyMMddhhmmss")
	public Date getDateStamp() {
		return date;
	}

	public Long getConceptId() {
		return conceptId;
	}

	public void setConceptId(Long conceptId) {
		this.conceptId = conceptId;
	}

	public String getConceptTerm() {
		if (conceptTerm == null) {
			return null;
		}
		return conceptTerm.getTerm() != null ? conceptTerm.getTerm() : conceptId.toString();
	}

	public void setConceptTerm(TermHolder conceptTerm) {
		this.conceptTerm = conceptTerm;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ClinicalEncounter that = (ClinicalEncounter) o;

		if (!date.equals(that.date)) return false;
		return conceptId.equals(that.conceptId);
	}

	@Override
	public int hashCode() {
		int result = date.hashCode();
		result = 31 * result + conceptId.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "ClinicalEncounter{" +
				", conceptId='" + conceptId + '\'' +
				", date=" + date +
				'}';
	}
}
