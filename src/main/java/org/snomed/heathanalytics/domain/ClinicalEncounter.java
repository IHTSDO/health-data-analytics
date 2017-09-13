package org.snomed.heathanalytics.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;

import java.util.Date;

@Document(indexName = "clinc-en")
public class ClinicalEncounter implements Act {

	@Id
	private String id;

	@Field(index = FieldIndex.not_analyzed)
	private String roleId;

	@Field(index = FieldIndex.not_analyzed)
	private Date date;

	@Field(index = FieldIndex.not_analyzed)
	private Long conceptId;

	@Field(index = FieldIndex.not_analyzed)
	private ClinicalEncounterType type;

	@Transient
	private String conceptTerm;

	@Transient
	private boolean primaryExposure;

	public interface Fields {
		String ROLE_ID = "roleId";
		String DATE = "date";
		String CONCEPT_ID = "conceptId";
		String TYPE = "type";
	}

	public ClinicalEncounter() {
	}

	public ClinicalEncounter(String roleId, Date date, ClinicalEncounterType type, Long conceptId) {
		this.roleId = roleId;
		this.date = date;
		this.conceptId = conceptId;
		this.type = type;
	}

	public String getRoleId() {
		return roleId;
	}

	@Override
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
	public Date getDate() {
		return date;
	}

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMddhhmmss")
	public Date getDateStamp() {
		return date;
	}

	public Long getConceptId() {
		return conceptId;
	}

	public void setConceptId(Long conceptId) {
		this.conceptId = conceptId;
	}

	public ClinicalEncounterType getType() {
		return type;
	}

	public void setType(ClinicalEncounterType type) {
		this.type = type;
	}

	public String getConceptTerm() {
		return conceptTerm;
	}

	public void setConceptTerm(String conceptTerm) {
		this.conceptTerm = conceptTerm;
	}

	public boolean isPrimaryExposure() {
		return primaryExposure;
	}

	public void setPrimaryExposure(boolean primaryExposure) {
		this.primaryExposure = primaryExposure;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ClinicalEncounter that = (ClinicalEncounter) o;

		if (!roleId.equals(that.roleId)) return false;
		if (!date.equals(that.date)) return false;
		return conceptId.equals(that.conceptId);
	}

	@Override
	public int hashCode() {
		int result = roleId.hashCode();
		result = 31 * result + date.hashCode();
		result = 31 * result + conceptId.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "ClinicalEncounter{" +
				"roleId='" + roleId + '\'' +
				", conceptId='" + conceptId + '\'' +
				", date=" + date +
				'}';
	}
}
