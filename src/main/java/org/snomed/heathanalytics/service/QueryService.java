package org.snomed.heathanalytics.service;

import org.ihtsdo.otf.sqs.service.SnomedQueryService;
import org.ihtsdo.otf.sqs.service.dto.ConceptIdResults;
import org.ihtsdo.otf.sqs.service.dto.ConceptResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.domain.ClinicalEncounter;
import org.snomed.heathanalytics.domain.Patient;
import org.snomed.heathanalytics.pojo.Stats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

@Service
public class QueryService {

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Autowired
	private SnomedQueryService snomedQueryService;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public Page<ClinicalEncounter> fetchCohort(String ecl) throws ServiceException {
		long start = new Date().getTime();

		try {
			ConceptIdResults conceptResults = snomedQueryService.eclQueryReturnConceptIdentifiers(ecl, 0, 1000 * 1000);
			List<Long> conceptIds = conceptResults.getConceptIds();

			long gatherConcepts = new Date().getTime() - start;
			start = new Date().getTime();

			Page<ClinicalEncounter> page = elasticsearchTemplate.queryForPage(new NativeSearchQueryBuilder()
							.withQuery(boolQuery()
									.should(termsQuery("conceptId", conceptIds))
							)
							.withPageable(new PageRequest(0, 100))
							.build(),
					ClinicalEncounter.class
			);
			logger.info("Fetched cohort for {} with {} results. Gathering concepts took {} mills, Query took {} mills",
					ecl, page.getTotalElements(), gatherConcepts, new Date().getTime() - start);
			return page;
		} catch (org.ihtsdo.otf.sqs.service.exception.ServiceException e) {
			throw new ServiceException("Failed to process ECL query.", e);
		}
	}

	public ConceptResults findConcepts(String prefix, int offset, int limit) throws ServiceException {
		try {
			return snomedQueryService.search(prefix, offset, limit);
		} catch (org.ihtsdo.otf.sqs.service.exception.ServiceException e) {
			throw new ServiceException("Failed to find concept by prefix '" + prefix + "'", e);
		}
	}

	public Stats getStats() {
		SearchQuery searchQuery = new NativeSearchQueryBuilder().build();
		long patientCount = elasticsearchTemplate.count(searchQuery, Patient.class);
		long clinicalEncounterCount = elasticsearchTemplate.count(searchQuery, ClinicalEncounter.class);
		return new Stats(new Date(), patientCount, clinicalEncounterCount);
	}

	public void setSnomedQueryService(SnomedQueryService snomedQueryService) {
		this.snomedQueryService = snomedQueryService;
	}
}
