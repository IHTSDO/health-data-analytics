import { ChangeDetectorRef, Component, ViewChild } from '@angular/core';
import { ChartComponent } from 'ng-apexcharts';
import { ReportService, ReportResponse } from '../../services/report.service';

@Component({
	selector: 'app-report-chart',
	templateUrl: './report-chart.component.html',
	styleUrls: ['./report-chart.component.scss'],
	standalone: false,
})
export class ReportChartComponent {
	@ViewChild('outcomeChart') outcomeChart?: ChartComponent;

	hideChart = true;
	series: any[] = [];
	chart: any = {
		type: 'bar',
		height: 450,
	};
	plotOptions: any = {
		bar: {
			borderRadius: 4,
			horizontal: true,
		},
	};
	dataLabels: any = {
		enabled: true,
	};
	xaxis: any = {
		title: { text: 'Correlation Percent' },
		categories: [],
		min: 0,
		max: 100,
	};
	tooltip: any = {
		y: {
			formatter: (value: number) => `${value}%`,
		},
	};

	constructor(
		private reportApi: ReportService,
		private cdr: ChangeDetectorRef
	) {}

	fetchReport(report: Record<string, unknown>): void {
		console.log('updating outcome stats', report);
		this.hideChart = false;
		this.cdr.detectChanges();
		this.reportApi.runReport(report).subscribe({
			next: (data) => this.applyReportData(data, report['colors'] as string[] | undefined),
		});
	}

	private applyReportData(data: ReportResponse, colors?: string[]): void {
		const labels: string[] = [];
		let series: Array<{ name?: string; data: number[] }> = [];
		if (!data.groups) {
			data.groups = [];
		}
		data.groups.forEach((group) => {
			let i = 0;
			labels.push(group.name);
			if (group.groups) {
				group.groups.forEach(() => {
					if (series.length <= i) {
						series.push({ data: [] });
					}
					series[i].data.push(0);
					i++;
				});
			}
		});
		this.series = series;
		const chartRef = this.outcomeChart;
		if (!chartRef) {
			return;
		}
		void chartRef.updateOptions(
			{
				xaxis: { categories: labels, min: 0, max: 100 },
				colors,
			},
			true,
			true,
			true
		);
		void chartRef.updateSeries(series, false);

		setTimeout(() => {
			series = [];
			data.groups.forEach((group) => {
				let i = 0;
				group.groups.forEach((subGroup) => {
					if (series.length <= i) {
						series.push({
							name: subGroup.name,
							data: [],
						});
					}
					let percent = (subGroup.patientCount / group.patientCount) * 100;
					percent = Math.round(percent * 100) / 100;
					series[i].data.push(percent);
					i++;
				});
			});
			console.log('series', series);
			void chartRef.updateSeries(series, true);
		}, 200);
	}
}
