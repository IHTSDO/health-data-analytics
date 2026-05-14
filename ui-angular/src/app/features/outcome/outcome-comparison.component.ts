import { Component, ViewChild } from '@angular/core';
import { PatientCriteriaModel } from '../../models/patient-criteria.model';
import { ClinicalEventCriterionModel } from '../../models/clinical-event-criterion.model';
import { ReportChartComponent } from '../../shared/report-chart/report-chart.component';
import { CorrelationService } from '../../services/correlation.service';

interface NamedGroup {
	name: string;
	criteria: PatientCriteriaModel;
}

@Component({
	selector: 'app-outcome-comparison',
	templateUrl: './outcome-comparison.component.html',
	styleUrls: ['./outcome-comparison.component.scss'],
	standalone: false,
})
export class OutcomeComparisonComponent {
	@ViewChild(ReportChartComponent) chart?: ReportChartComponent;

	cohortCriteria = new PatientCriteriaModel();
	groups: NamedGroup[] = [];
	includeGroupNoneOfAbove = false;
	outcomes: ClinicalEventCriterionModel[] = [];
	discovering = false;

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

	constructor(private correlationApi: CorrelationService) {}

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
		this.groups.push({ name: '', criteria: new PatientCriteriaModel() });
	}

	discoverGroups(): void {
		console.log('Discover Correlations...');
		this.discovering = true;
		const outcome = this.outcomes[0];
		const reportRequest: Record<string, unknown> = {
			baseCriteria: this.cohortCriteria.getForAPI(),
			negativeOutcomeECL: outcome.conceptECL,
		};
		this.correlationApi.discover(reportRequest).subscribe({
			next: (response) => {
				this.discovering = false;
				for (const node of response.nodes) {
					const patientCriteriaModel = new PatientCriteriaModel();
					const eventCriteria = new ClinicalEventCriterionModel(node.label, '<<' + node.conceptId);
					eventCriteria.conceptECL = `<<${node.conceptId} |${node.label}|`;
					eventCriteria.display = node.label;
					patientCriteriaModel.eventCriteria.push(eventCriteria);
					this.groups.push({ name: `${node.label}*`, criteria: patientCriteriaModel });
				}
			},
			error: () => {
				this.discovering = false;
			},
		});
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

		if (this.includeGroupNoneOfAbove) {
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
