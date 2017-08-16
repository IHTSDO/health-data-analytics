package org.snomed.heathanalytics.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.Date;
import java.util.Set;

@Document(indexName = "clinc-en")
public class ClinicalEncounter implements Act {

	@Id
	private String id;
	private String roleId;
	private Date date;
	private Long conceptId;

	@Transient
	private String conceptTerm;

	@Transient
	private boolean primaryExposure;

	public static final String FIELD_ROLE_ID = "roleId";
	public static final String FIELD_DATE = "date";
	public static final String FIELD_CONCEPT_ID = "conceptId";

	public ClinicalEncounter() {
	}

	public ClinicalEncounter(String roleId, Date date, Long conceptId) {
		this.roleId = roleId;
		this.date = date;
		this.conceptId = conceptId;
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
