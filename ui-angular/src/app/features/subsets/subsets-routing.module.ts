import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SubsetsComponent } from './subsets.component';

const routes: Routes = [
	{ path: '', component: SubsetsComponent },
	{ path: ':id', component: SubsetsComponent },
];

@NgModule({
	imports: [RouterModule.forChild(routes)],
	exports: [RouterModule],
})
export class SubsetsRoutingModule {}
