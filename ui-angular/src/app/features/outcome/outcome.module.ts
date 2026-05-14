import { NgModule } from '@angular/core';
import { OutcomeComparisonComponent } from './outcome-comparison.component';
import { OutcomeRoutingModule } from './outcome-routing.module';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
	declarations: [OutcomeComparisonComponent],
	imports: [SharedModule, OutcomeRoutingModule],
})
export class OutcomeModule {}
