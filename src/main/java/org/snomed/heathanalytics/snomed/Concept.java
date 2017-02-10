package org.snomed.heathanalytics.snomed;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Set;

@Document(indexName = "concept")
public class Concept {

	@Id
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed)
	private String conceptId;

	@Field(type = FieldType.String, index = FieldIndex.not_analyzed)
	private String fsn;

	@Field(type = FieldType.String, index = FieldIndex.not_analyzed)
	private String definitionStatusId;

	private Set<Long> ancestors;

	public Concept() {
	}

	public Concept(Long conceptId, String fsn, String definitionStatusId, Set<Long> ancestors) {
		this.conceptId = conceptId.toString();
		this.fsn = fsn;
		this.definitionStatusId = definitionStatusId;
		this.ancestors = ancestors;
	}

	public Long getConceptId() {
		return Long.getLong(conceptId);
	}

	public String getFsn() {
		return fsn;
	}

	public String getDefinitionStatusId() {
		return definitionStatusId;
	}

	public Set<Long> getAncestors() {
		return ancestors;
	}

}
