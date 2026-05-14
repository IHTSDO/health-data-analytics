import { ChangeDetectorRef, Component, ViewChild } from '@angular/core';
import { ChartComponent } from 'ng-apexcharts';
import { firstValueFrom } from 'rxjs';
import { ReportService, ReportResponse } from '../../services/report.service';

@Component({
	selector: 'app-report-chart-longitudinal',
	templateUrl: './report-chart-longitudinal.component.html',
	styleUrls: ['./report-chart-longitudinal.component.scss'],
	standalone: false,
})
export class ReportChartLongitudinalComponent {
	@ViewChild('outcomeChart') outcomeChart?: ChartComponent;

	createChart = false;
	series: any[] = [];
	chart: any = {
		height: 350,
		type: 'line',
		stacked: false,
	};
	dataLabels: any = {
		enabled: false,
	};
	colors = ['#FA8989', '#FF924C', '#FFCA3A', '#C5CA30', '#8AC926', '#52A675', '#1982C4', '#4267AC', '#6A4C93'];
	markers: any = {
		size: 4,
	};
	stroke: any = {
		width: [4, 4, 4, 4, 4, 4, 4, 4, 4],
	};
	plotOptions: any = {
		bar: {
			columnWidth: '20%',
		},
	};
	xaxis: any = {
		categories: [],
	};
	yaxis: any = [
		{
			seriesName: 'Column A',
			axisTicks: { show: true },
			axisBorder: { show: true },
			title: { text: 'Patients' },
		},
	];
	tooltip: any = {
		shared: false,
		intersect: true,
		x: { show: false },
	};
	legend: any = {
		horizontalAlign: 'left',
		offsetX: 40,
	};

	constructor(
		private reportApi: ReportService,
		private cdr: ChangeDetectorRef
	) {}

	async fetchReports(reports: Array<Record<string, unknown>>): Promise<void> {
		console.log('fetchReports');
		const categories = reports.map((r) => r['name'] as string);
		this.xaxis = { ...this.xaxis, categories };
		this.createChart = true;
		this.cdr.detectChanges();

		let initial = true;
		for (let index = 0; index < reports.length; index++) {
			await this.fetchReport(reports[index], initial);
			initial = false;
		}
	}

	async fetchReport(report: Record<string, unknown>, initial: boolean): Promise<void> {
		console.log('updating outcome stats', report);
		const data = await firstValueFrom(this.reportApi.runReport(report));

		if (initial) {
			console.log('initial graph');
			const nextSeries: Array<{ name?: string; data: number[] }> = [];
			const typed = data as ReportResponse;
			if (!typed.groups) {
				typed.groups = [];
			}
			typed.groups.forEach((group) => {
				group.groups.forEach((subgroup) => {
					nextSeries.push({
						name: subgroup.name,
						data: [],
					});
				});
			});
			this.series = nextSeries;
		}

		if (!data.groups) {
			data.groups = [];
		}

		setTimeout(() => {
			const typedData = data as ReportResponse;
			const seriesLocal = this.series as Array<{ name?: string; data: number[] }>;
			typedData.groups.forEach((group) => {
				let i = 0;
				group.groups.forEach((subGroup) => {
					seriesLocal[i].data.push(subGroup.patientCount);
					const chartRef = this.outcomeChart;
					if (chartRef) {
						void chartRef.updateSeries(this.series, true);
					}
					i++;
				});
			});
		}, 200);
	}
}
