import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { NgApexchartsModule } from 'ng-apexcharts';
import { BsDropdownModule } from 'ngx-bootstrap/dropdown';

import { AddCriteriaDropdownComponent } from './add-criteria-dropdown/add-criteria-dropdown.component';
import { ClinicalEventCriterionComponent } from './clinical-event-criterion/clinical-event-criterion.component';
import { PatientCriteriaComponent } from './patient-criteria/patient-criteria.component';
import { ReportChartComponent } from './report-chart/report-chart.component';
import { ReportChartLongitudinalComponent } from './report-chart-longitudinal/report-chart-longitudinal.component';

const EXPORT_DECLARATIONS = [
	AddCriteriaDropdownComponent,
	ClinicalEventCriterionComponent,
	PatientCriteriaComponent,
	ReportChartComponent,
	ReportChartLongitudinalComponent,
];

@NgModule({
	declarations: EXPORT_DECLARATIONS,
	imports: [
		CommonModule,
		FormsModule,
		RouterModule,
		NgApexchartsModule,
		BsDropdownModule,
	],
	exports: [...EXPORT_DECLARATIONS, CommonModule, FormsModule, RouterModule],
})
export class SharedModule {}
