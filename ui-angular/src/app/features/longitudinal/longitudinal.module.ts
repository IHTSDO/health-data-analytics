import { NgModule } from '@angular/core';
import { LongitudinalComponent } from './longitudinal.component';
import { LongitudinalRoutingModule } from './longitudinal-routing.module';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
	declarations: [LongitudinalComponent],
	imports: [SharedModule, LongitudinalRoutingModule],
})
export class LongitudinalModule {}
