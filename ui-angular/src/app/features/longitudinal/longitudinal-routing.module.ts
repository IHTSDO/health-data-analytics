import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LongitudinalComponent } from './longitudinal.component';

const routes: Routes = [{ path: '', component: LongitudinalComponent }];

@NgModule({
	imports: [RouterModule.forChild(routes)],
	exports: [RouterModule],
})
export class LongitudinalRoutingModule {}
