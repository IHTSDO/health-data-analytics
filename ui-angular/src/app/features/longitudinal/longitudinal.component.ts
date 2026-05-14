import { Component, ViewChild } from '@angular/core';
import { PatientCriteriaModel } from '../../models/patient-criteria.model';
import { ClinicalEventCriterionModel } from '../../models/clinical-event-criterion.model';
import { ReportChartLongitudinalComponent } from '../../shared/report-chart-longitudinal/report-chart-longitudinal.component';

@Component({
	selector: 'app-longitudinal',
	templateUrl: './longitudinal.component.html',
	styleUrls: ['./longitudinal.component.scss'],
	standalone: false,
})
export class LongitudinalComponent {
	@ViewChild(ReportChartLongitudinalComponent) chart?: ReportChartLongitudinalComponent;

	cohortCriteria = new PatientCriteriaModel();
	startDate = '';
	endDate = '';
	granularity: 'YEAR' | 'MONTH' | 'DAY' | 'HOUR' = 'YEAR';
	outcomes: ClinicalEventCriterionModel[] = [];
	outcomesIncludeHistory = false;
	readonly eclHistory = ' {{ +HISTORY }}';

	onHistoryToggle(): void {
		this.outcomes.forEach((outcome) => {
			outcome.historyECL = this.outcomesIncludeHistory ? this.eclHistory : '';
		});
	}

	addOutcome(display: string, eclBinding: string): void {
		const model = new ClinicalEventCriterionModel(display, eclBinding);
		if (this.outcomesIncludeHistory) {
			model.historyECL = this.eclHistory;
		}
		this.outcomes.push(model);
	}

	removeOutcome(outcome: ClinicalEventCriterionModel): void {
		const index = this.outcomes.indexOf(outcome);
		if (index >= 0) {
			this.outcomes.splice(index, 1);
		}
	}

	get timepoints(): Date[] {
		const points: Date[] = [];
		let current = this.parseDate(this.startDate);
		const end = this.parseDate(this.endDate);
		if (!current || !end) {
			return points;
		}
		let i = 0;
		while (current.getTime() <= end.getTime() && i++ < 1000) {
			points.push(new Date(current.getTime()));
			if (this.granularity === 'YEAR') {
				current.setUTCFullYear(current.getUTCFullYear() + 1);
			} else if (this.granularity === 'MONTH') {
				current.setUTCMonth(current.getUTCMonth() + 1);
			} else if (this.granularity === 'DAY') {
				current.setUTCDate(current.getUTCDate() + 1);
			} else if (this.granularity === 'HOUR') {
				current.setUTCHours(current.getUTCHours() + 1);
			}
		}
		return points;
	}

	run(): void {
		const reportRequests: Record<string, unknown>[] = [];
		let startTimepoint: Date | undefined;
		this.timepoints.forEach((endTimepoint) => {
			if (startTimepoint) {
				reportRequests.push(
					this.getReportRequest(startTimepoint, endTimepoint, `${startTimepoint.getUTCFullYear()}`)
				);
			}
			startTimepoint = endTimepoint;
		});
		void this.chart?.fetchReports(reportRequests);
	}

	getReportRequest(startDate: Date, endDate: Date, label: string): Record<string, unknown> {
		const report: Record<string, unknown> = {};
		report['name'] = label;
		report['criteria'] = this.cohortCriteria.getForAPI();

		const patientGroups: Record<string, unknown>[] = [];
		patientGroups.push({});

		const outcomesRequest: Record<string, unknown>[] = [];
		const colors: string[] = [];
		this.outcomes.forEach((outcome) => {
			if (outcome.isFilled()) {
				colors.push(outcome.color);
				outcomesRequest.push({
					name: outcome.display,
					criteria: {
						eventCriteria: [outcome.getForAPI(startDate, endDate)],
					},
				});
			}
		});
		report['groups'] = [patientGroups, outcomesRequest];
		report['colors'] = colors;
		return report;
	}

	private parseDate(input: string): Date | null {
		const regex = /[0-9]{4}-[0-9]{2}-[0-9]{2}/;
		if (input && regex.test(input)) {
			const parts = input.split('-');
			return new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
		}
		return null;
	}
}
