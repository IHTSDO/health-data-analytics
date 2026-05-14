import { Component, ViewChild } from '@angular/core';
import { PatientCriteriaModel } from '../../models/patient-criteria.model';
import { ClinicalEventCriterionModel } from '../../models/clinical-event-criterion.model';
import { ReportChartComponent } from '../../shared/report-chart/report-chart.component';

interface NamedGroup {
	name: string;
	criteria: PatientCriteriaModel;
}

@Component({
	selector: 'app-treatment-comparison',
	templateUrl: './treatment-comparison.component.html',
	styleUrls: ['./treatment-comparison.component.scss'],
	standalone: false,
})
export class TreatmentComparisonComponent {
	@ViewChild(ReportChartComponent) chart?: ReportChartComponent;

	cohortCriteria = new PatientCriteriaModel();
	groups: NamedGroup[] = [];
	includeNoTreatmentOption = false;
	outcomes: ClinicalEventCriterionModel[] = [];

	private readonly colors = [
		'#FA8989',
		'#FF924C',
		'#FFCA3A',
		'#C5CA30',
		'#8AC926',
		'#52A675',
		'#1982C4',
		'#4267AC',
		'#6A4C93',
	];

	addOutcome(display: string, eclBinding: string): void {
		const outcome = new ClinicalEventCriterionModel(display, eclBinding);
		const colorsUsed = this.outcomes.map((o) => o.color).filter(Boolean);
		const colorsLeft = this.colors.filter((c) => !colorsUsed.includes(c));
		if (colorsLeft.length !== 0) {
			outcome.color = colorsLeft[0];
		}
		this.outcomes.push(outcome);
	}

	addGroup(): void {
		const model = new PatientCriteriaModel();
		model.treatment = true;
		this.groups.push({ name: 'Treatment X', criteria: model });
	}

	runReport(): void {
		const reportRequest = this.getReportRequest();
		this.updateOutcomes(reportRequest);
	}

	private updateOutcomes(report: Record<string, unknown>): void {
		const groups = report['groups'] as unknown[];
		if (groups && groups.length === 2 && (groups[1] as unknown[]).length) {
			this.chart?.fetchReport(report);
		}
	}

	getReportRequest(): Record<string, unknown> {
		const report: Record<string, unknown> = {};
		report['criteria'] = this.cohortCriteria.getForAPI();

		const patientGroups: Record<string, unknown>[] = [];
		this.groups.forEach((group) => {
			patientGroups.push({
				name: group.name,
				criteria: group.criteria.getForAPI(),
			});
		});

		if (this.includeNoTreatmentOption) {
			const exclusionCriteria: unknown[] = [];
			patientGroups.forEach((patientGroup) => {
				exclusionCriteria.push(patientGroup['criteria']);
			});
			patientGroups.push({
				name: 'All other patients',
				criteria: { exclusionCriteria },
			});
		}

		const outcomesRequest: Record<string, unknown>[] = [];
		const colors: string[] = [];
		this.outcomes.forEach((outcome) => {
			if (outcome.isFilled()) {
				colors.push(outcome.color);
				outcome.withinDaysAfterPreviouslyMatchedEvent = -1;
				outcomesRequest.push({
					criteria: {
						eventCriteria: [outcome.getForAPI()],
					},
					name: outcome.display,
				});
			}
		});
		report['groups'] = [patientGroups, outcomesRequest];
		report['colors'] = colors;
		return report;
	}
}
