import { NgModule } from '@angular/core';
import { SubsetsComponent } from './subsets.component';
import { SubsetsRoutingModule } from './subsets-routing.module';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
	declarations: [SubsetsComponent],
	imports: [SharedModule, SubsetsRoutingModule],
})
export class SubsetsModule {}
