package org.snomed.heathanalytics.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import org.snomed.heathanalytics.model.pojo.TermHolder;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class ClinicalEvent {

	@Field(type = FieldType.Long)
	private long dateLong;

	@Field(type = FieldType.Keyword)
	private Long conceptId;

	@Field(type = FieldType.Keyword)
	private String conceptDate;

	@Transient
	private TermHolder conceptTerm;

	public interface Fields {
		String ROLE_ID = "roleId";
		String DATE_LONG = "dateLong";
		String CONCEPT_ID = "conceptId";
	}

	public ClinicalEvent() {
	}

	public ClinicalEvent(Date date, Long conceptId) {
		dateLong = date.getTime();
		this.conceptId = conceptId;
		updateConceptDate();
	}

	public void updateConceptDate() {
		conceptDate = conceptId + "," + dateLong;
	}

	public ClinicalEvent(Calendar date, Long conceptId) {
		this(date.getTime(), conceptId);
	}

	@JsonView(View.Elasticsearch.class)
	public long getDateLong() {
		return dateLong;
	}

	public void setDateLong(long dateLong) {
		this.dateLong = dateLong;
	}

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'hh:mm:ssz", timezone = "UTC")
	@JsonView(View.API.class)
	public Date getDate() {
		return new Date(dateLong);
	}

	public void setDate(Date date) {
		setDateLong(date.getTime());
	}

	@JsonView({View.API.class, View.Elasticsearch.class})
	public Long getConceptId() {
		return conceptId;
	}

	public void setConceptId(Long conceptId) {
		this.conceptId = conceptId;
	}

	@JsonView({View.Elasticsearch.class})
	public String getConceptDate() {
		updateConceptDate();
		return conceptDate;
	}

	@JsonView(View.API.class)
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
	public String toString() {
		return "ClinicalEvent{" +
				"conceptId='" + conceptId + '\'' +
				", dateLong=" + dateLong +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ClinicalEvent event = (ClinicalEvent) o;
		return dateLong == event.dateLong &&
				Objects.equals(conceptId, event.conceptId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dateLong, conceptId);
	}
}
