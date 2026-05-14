import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

const routes: Routes = [
	{
		path: '',
		loadChildren: () => import('./features/home/home.module').then((m) => m.HomeModule),
	},
	{
		path: 'subsets',
		loadChildren: () => import('./features/subsets/subsets.module').then((m) => m.SubsetsModule),
	},
	{
		path: 'treatment-comparison',
		loadChildren: () =>
			import('./features/treatment/treatment.module').then((m) => m.TreatmentModule),
	},
	{
		path: 'group-comparison',
		loadChildren: () => import('./features/outcome/outcome.module').then((m) => m.OutcomeModule),
	},
	{
		path: 'longitudinal-comparison',
		loadChildren: () =>
			import('./features/longitudinal/longitudinal.module').then((m) => m.LongitudinalModule),
	},
];

@NgModule({
	imports: [RouterModule.forRoot(routes)],
	exports: [RouterModule],
})
export class AppRoutingModule {}
