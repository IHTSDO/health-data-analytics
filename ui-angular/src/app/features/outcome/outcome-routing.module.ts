import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { OutcomeComparisonComponent } from './outcome-comparison.component';

const routes: Routes = [{ path: '', component: OutcomeComparisonComponent }];

@NgModule({
	imports: [RouterModule.forChild(routes)],
	exports: [RouterModule],
})
export class OutcomeRoutingModule {}
