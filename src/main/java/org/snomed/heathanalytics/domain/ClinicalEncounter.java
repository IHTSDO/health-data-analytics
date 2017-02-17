package org.snomed.heathanalytics.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.Date;
import java.util.Set;

@Document(indexName = "clinc-en")
public class ClinicalEncounter implements Act {

	@Id
	private String id;
	private String roleId;
	private Date date;
	private String conceptId;
	private Set<Long> transitiveClosure;

	public ClinicalEncounter() {
	}

	public ClinicalEncounter(String roleId, Date date, String conceptId, Set<Long> transitiveClosure) {
		this.roleId = roleId;
		this.date = date;
		this.conceptId = conceptId;
		this.transitiveClosure = transitiveClosure;
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

	public Set<Long> getTransitiveClosure() {
		return transitiveClosure;
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
