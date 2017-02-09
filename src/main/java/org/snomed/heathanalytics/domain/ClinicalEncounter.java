package org.snomed.heathanalytics.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.Date;

@Document(indexName = "clinc-en")
public class ClinicalEncounter implements Act {

	@Id
	private String id;
	private String roleId;
	private Date date;
	private String conceptId;

	public ClinicalEncounter() {
	}

	public ClinicalEncounter(String roleId, Date date, String conceptId) {
		this.roleId = roleId;
		this.date = date;
		this.conceptId = conceptId;
	}

	public String getRoleId() {
		return roleId;
	}

	@Override
	public Date getDate() {
		return date;
	}

	public String getConceptId() {
		return conceptId;
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
