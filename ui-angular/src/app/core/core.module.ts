import { NgModule, Optional, SkipSelf } from '@angular/core';
import { HttpClientModule } from '@angular/common/http';

@NgModule({
	imports: [HttpClientModule],
	exports: [HttpClientModule],
})
export class CoreModule {
	constructor(@Optional() @SkipSelf() parent?: CoreModule) {
		if (parent) {
			throw new Error('CoreModule should only be imported in AppModule.');
		}
	}
}
