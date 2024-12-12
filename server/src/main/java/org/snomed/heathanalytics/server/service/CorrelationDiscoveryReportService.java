package org.snomed.heathanalytics.server.service;

import io.kaicode.graphpattern.GraphClustering;
import io.kaicode.graphpattern.domain.GraphBuilder;
import io.kaicode.graphpattern.domain.Node;
import org.snomed.heathanalytics.model.ClinicalEvent;
import org.snomed.heathanalytics.model.Patient;
import org.snomed.heathanalytics.server.model.*;
import org.snomed.heathanalytics.server.model.correlation.CorrelationDiscoveryReportDefinition;
import org.snomed.heathanalytics.server.model.correlation.CorrelationDiscoveryReportResult;
import org.snomed.heathanalytics.server.model.correlation.CorrelationDiscoveryReportResultNode;
import org.snomed.heathanalytics.server.pojo.ConceptResult;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CorrelationDiscoveryReportService {

	private final PatientQueryService patientQueryService;
	private final SnomedService terminologyService;

	public CorrelationDiscoveryReportService(PatientQueryService patientQueryService, SnomedService terminologyService) {
		this.patientQueryService = patientQueryService;
		this.terminologyService = terminologyService;
	}

	public CorrelationDiscoveryReportResult runCorrelationDiscoveryReport(CorrelationDiscoveryReportDefinition reportDefinition) throws ServiceException {
		Timer timer = new Timer("CorrelationDiscovery");
		CohortCriteria patientCriteria = reportDefinition.baseCriteria();
		String negativeOutcomeECL = reportDefinition.negativeOutcomeECL();
		List<Long> negativeOutcomeCodes = terminologyService.getConceptIds(negativeOutcomeECL);
		if (negativeOutcomeCodes.isEmpty()) {
			throw new ServiceException("No high risk condition codes found");
		}

		int size = 10_000;
		List<Patient> allPatients = new ArrayList<>();
		PatientPageWithSearchAfter patientPage = null;
		int page = 0;
		do {
			if (patientPage == null) {
				patientPage = (PatientPageWithSearchAfter) patientQueryService.fetchCohort(patientCriteria, page++, size);
			} else {
				String searchAfter = patientPage.getSearchAfter();
				patientPage = (PatientPageWithSearchAfter) patientQueryService.fetchCohort(patientCriteria, searchAfter, size);
				page++;
			}
			allPatients.addAll(patientPage.getContent());
			timer.checkpoint("Load patient page %s of %s (%s)".formatted(page, patientPage.getTotalPages(), (page / patientPage.getTotalPages()) * 100));
//		} while (allPatients.size() < patientPage.getTotalElements());
		} while (allPatients.size() < patientPage.getTotalElements() && allPatients.size() < 10_000);

		Set<Long> allConcepts = new HashSet<>();
		for (Patient patient : allPatients) {
			for (ClinicalEvent event : patient.getEvents()) {
				allConcepts.add(event.getConceptId());
			}
		}
		List<SnomedService.NodeWithParents> nodesWithParents = terminologyService.loadPartialHierarchy(allConcepts);
		timer.checkpoint("Load partial hierarchy of %s concepts".formatted(nodesWithParents.size()));
		GraphBuilder graphBuilder = new GraphBuilder();
		for (SnomedService.NodeWithParents nodeWithParents : nodesWithParents) {
			for (String parent : nodeWithParents.parents()) {
				graphBuilder.addChildParentLink(nodeWithParents.code(), parent);
			}
		}
		timer.checkpoint("Build graph structure");

		Map<String, Set<String>> patientEvents = new HashMap<>();
		Set<String> highRiskConditionCodes = negativeOutcomeCodes.stream().map(Objects::toString).collect(Collectors.toSet());


		Set<String> groupBPatients = new HashSet<>();
		for (Patient patient : allPatients) {
			Set<String> events = patient.getEvents().stream().map(ClinicalEvent::getConceptId).map(Objects::toString).collect(Collectors.toSet());
			patientEvents.put(patient.getRoleId(), events);
			for (String event : events) {
				if (highRiskConditionCodes.contains(event)) {
					groupBPatients.add(patient.getRoleId());
					break;
				}

			}
		}

		highRiskConditionCodes = new HashSet<>(highRiskConditionCodes);
		highRiskConditionCodes.add("419099009");// 419099009 |Dead (finding)|

		GraphClustering graphClustering = new GraphClustering(0.1f, 0.05f, 10);
		List<Node> strongestCorrelatingPhenotypes = graphClustering.run(graphBuilder, patientEvents, groupBPatients, highRiskConditionCodes);
		for (Node strongestCorrelatingPhenotype : strongestCorrelatingPhenotypes) {
			ConceptResult concept = terminologyService.findConcept(strongestCorrelatingPhenotype.getCode());
			if (concept != null) {
				strongestCorrelatingPhenotype.setLabel(concept.getDisplay());
			}
			System.out.println(strongestCorrelatingPhenotype);
		}
		timer.finish();
		System.out.println("Done");

		List<CorrelationDiscoveryReportResultNode> reportNodes = new ArrayList<>();
		for (Node node : strongestCorrelatingPhenotypes) {
			float score = node.getDepthBoostedAggregateGroupDifferenceBackup();
			if (score > 0) {// Ignore negative scores for now
				reportNodes.add(new CorrelationDiscoveryReportResultNode(node.getCode(), node.getLabel(), score));
			}
		}

		return new CorrelationDiscoveryReportResult(reportNodes);
	}

}
