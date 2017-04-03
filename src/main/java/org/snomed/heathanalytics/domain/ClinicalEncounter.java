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

	public Long getConceptId() {
		return conceptId;
	}

	public String getConceptTerm() {
		return conceptTerm;
	}

	public void setConceptTerm(String conceptTerm) {
		this.conceptTerm = conceptTerm;
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
