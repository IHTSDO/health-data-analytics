import { NgModule } from '@angular/core';
import { TreatmentComparisonComponent } from './treatment-comparison.component';
import { TreatmentRoutingModule } from './treatment-routing.module';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
	declarations: [TreatmentComparisonComponent],
	imports: [SharedModule, TreatmentRoutingModule],
})
export class TreatmentModule {}
