package org.snomed.heathanalytics.server.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "subset")
public class Subset {

	@Id
	private String id;
	private String name;
	private String description;
	private String eclModel;
	private String ecl;

	// TODO: Consider adding username field to avoid multi-user conflict.

	public static final class Fields {
		public static final String ID = "id";
		public static final String NAME = "name";
		public static final String DESCRIPTION = "description";
		public static final String ECL_MODEL = "eclModel";
		public static final String ECL = "ecl";
	}

	public Subset() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getEclModel() {
		return eclModel;
	}

	public void setEclModel(String eclModel) {
		this.eclModel = eclModel;
	}

	public String getEcl() {
		return ecl;
	}

	public void setEcl(String ecl) {
		this.ecl = ecl;
	}
}
