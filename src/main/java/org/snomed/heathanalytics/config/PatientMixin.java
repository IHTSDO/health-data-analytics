package org.snomed.heathanalytics.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.snomed.heathanalytics.domain.ClinicalEncounter;

import java.util.Set;

public abstract class PatientMixin {

	@JsonIgnore
	abstract String getDobFormated();

	@JsonIgnore
	abstract Set<ClinicalEncounter> getEncounters();

}
