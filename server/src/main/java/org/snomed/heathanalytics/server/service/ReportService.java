package org.snomed.heathanalytics.server.service;

import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.model.Patient;
import org.snomed.heathanalytics.server.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class ReportService {

	@Autowired
	private PatientQueryService patientQueryService;

	private final ExecutorService executorService = Executors.newFixedThreadPool(4);

	public Report runReport(ReportDefinition reportDefinition) throws ServiceException {
		Timer timer = new Timer();
		// Fetch page of patients matching top level criteria
		CohortCriteria patientCriteria = reportDefinition.getCriteria();
		int count;
		if (patientCriteria != null) {
			count = patientQueryService.fetchCohortCount(patientCriteria);
		} else {
			count = (int) patientQueryService.getStats().getPatientCount();
		}
		timer.split("cohort count");
		Report report = new Report(reportDefinition.getName(), count, patientCriteria);

		List<List<SubReportDefinition>> subGroupLists = reportDefinition.getGroups();
		addReportGroups(report, subGroupLists, 0, patientCriteria, timer);

		LoggerFactory.getLogger(getClass()).info("Times: {}", timer.getTimes());
		return report;
	}

	public StatisticalCorrelationReport runStatisticalReport(StatisticalCorrelationReportDefinition reportDefinition) throws ServiceException {
		CohortCriteria patientCriteria = new CohortCriteria();

		// Copy base criteria
		patientCriteria.copyCriteriaWhereMoreSpecific(reportDefinition.getBaseCriteria());

		EncounterCriterion treatmentCriterion = reportDefinition.getTreatmentCriterion();
		InputValidationHelper.checkInput("treatmentCriterion is required for the statistical test.", treatmentCriterion != null);
		EncounterCriterion negativeOutcomeCriterion = reportDefinition.getNegativeOutcomeCriterion();
		InputValidationHelper.checkInput("negativeOutcomeCriterion is required for a statistical test.", negativeOutcomeCriterion != null);

		// A. Count patients WITH treatment, WITH negative outcome
		List<EncounterCriterion> encounterCriteria = patientCriteria.getEncounterCriteria();
		encounterCriteria.add(treatmentCriterion);
		encounterCriteria.add(negativeOutcomeCriterion);
		int withTreatmentWithNegativeOutcomeCount = patientQueryService.fetchCohortCount(patientCriteria);

		// B. Count patients WITH treatment
		removeLast(encounterCriteria);
		int withTreatmentCount = patientQueryService.fetchCohortCount(patientCriteria);

		// Has test variable chance of outcome = A / B

		// C. Count patients WITHOUT treatment, WITH negative outcome
		treatmentCriterion.setHas(false);
		encounterCriteria.add(negativeOutcomeCriterion);
		int withoutTreatmentWithNegativeOutcomeCount = patientQueryService.fetchCohortCount(patientCriteria);

		// D. Count patients WITHOUT test variable
		removeLast(encounterCriteria);
		int withoutTreatmentCount = patientQueryService.fetchCohortCount(patientCriteria);
		treatmentCriterion.setHas(true);// reset

		// Has not test variable chance of outcome = C / D

		return new StatisticalCorrelationReport(
				(int) patientQueryService.getStats().getPatientCount(),
				withTreatmentCount,
				withTreatmentWithNegativeOutcomeCount,
				withoutTreatmentCount,
				withoutTreatmentWithNegativeOutcomeCount);
	}


	private void addReportGroups(Report report, List<List<SubReportDefinition>> groupLists, int listsIndex, CohortCriteria patientCriteria, Timer timer) throws ServiceException {
		if (groupLists != null && groupLists.size() > listsIndex) {
			List<SubReportDefinition> groupList = groupLists.get(listsIndex);

			// Clear CPT Analysis flag of inherited criterion to allow a report focused on the most specific criteria
			if (patientCriteria != null) {
				patientCriteria = patientCriteria.clone();
				patientCriteria.getEncounterCriteria().forEach(encounterCriterion -> encounterCriterion.setIncludeCPTAnalysis(false));
			}

			List<Future<Page<Patient>>> futures = new ArrayList<>();
			List<CohortCriteria> combinedCriteriaList = new ArrayList<>();
			for (SubReportDefinition reportDefinition : groupList) {
				CohortCriteria combinedCriteria = combineCriteria(patientCriteria, reportDefinition.getCriteria());
				futures.add(executorService.submit(() -> patientQueryService.fetchCohort(combinedCriteria)));
				combinedCriteriaList.add(combinedCriteria);
			}
			try {
				for (int i = 0; i < futures.size(); i++) {
					Future<Page<Patient>> future = futures.get(i);
					SubReportDefinition reportDefinition = groupList.get(i);
					CohortCriteria combinedCriteria = combinedCriteriaList.get(i);
					Page<Patient> patientsPage = future.get();
					Map<String, CPTTotals> cptTotals = null;
					if (patientsPage instanceof PatientPageWithCPTTotals) {
						PatientPageWithCPTTotals pageWithEncounterCounts = (PatientPageWithCPTTotals) patientsPage;
						cptTotals = pageWithEncounterCounts.getCptTotals();
					}
					Report reportGroup = new Report(reportDefinition.getName(), (int) patientsPage.getTotalElements(), combinedCriteria, cptTotals);
					report.addGroup(reportGroup);
					addReportGroups(reportGroup, groupLists, listsIndex + 1, combinedCriteria, timer);

				}
			} catch (InterruptedException | ExecutionException e) {
				throw new ServiceException("Failed to fetch cohort or group.", e);
			}
		}
	}

	private CohortCriteria combineCriteria(CohortCriteria mainCriteria, CohortCriteria additionalCriteria) {
		CohortCriteria combinedCriteria = new CohortCriteria();
		combinedCriteria.copyCriteriaWhereMoreSpecific(mainCriteria);
		combinedCriteria.copyCriteriaWhereMoreSpecific(additionalCriteria);
		return combinedCriteria;
	}

	private void removeLast(List<EncounterCriterion> list) {
		list.remove(list.size() - 1);
	}

}
