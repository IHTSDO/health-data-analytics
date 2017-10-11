package org.snomed.heathanalytics.domain;

import org.springframework.data.annotation.Transient;

import java.util.UUID;

public class Criterion {

	@Transient
	private String id;// Id generated - only used as a key in maps.
	private String subsetId;
	private String ecl;

	public Criterion() {
		id = UUID.randomUUID().toString();
	}

	public Criterion(String ecl) {
		this();
		this.ecl = ecl;
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

		Criterion criterion = (Criterion) o;

		return id.equals(criterion.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
