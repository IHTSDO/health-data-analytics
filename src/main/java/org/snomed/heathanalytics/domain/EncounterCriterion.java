package org.snomed.heathanalytics.domain;

import org.springframework.data.annotation.Transient;

import java.util.UUID;

public class EncounterCriterion {

	@Transient
	private String id;// Id generated - only used as a key in maps.
	private boolean has;
	private String subsetId;
	private String ecl;

	public EncounterCriterion() {
		id = UUID.randomUUID().toString();
		has = true;
	}

	public EncounterCriterion(String ecl) {
		this();
		this.ecl = ecl;
	}

	public boolean isHas() {
		return has;
	}

	public void setHas(boolean has) {
		this.has = has;
	}

	public String getSubsetId() {
		return subsetId;
	}

	public void setSubsetId(String subsetId) {
		this.subsetId = subsetId;
	}

	public String getEcl() {
		return ecl;
	}

	public void setEcl(String ecl) {
		this.ecl = ecl;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		EncounterCriterion criterion = (EncounterCriterion) o;

		return id.equals(criterion.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
