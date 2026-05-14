import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class UiStateService {
	constructor(private http: HttpClient) {}

	getTreatments(): Observable<Record<string, unknown>> {
		return this.http.get<Record<string, unknown>>('api/ui-state/treatments/dev');
	}

	saveTreatments(model: Record<string, unknown>): Observable<unknown> {
		return this.http.post('api/ui-state/treatments/dev', model);
	}

	getGroups(): Observable<Record<string, unknown>> {
		return this.http.get<Record<string, unknown>>('api/ui-state/groups/dev');
	}

	saveGroups(model: Record<string, unknown>): Observable<unknown> {
		return this.http.post('api/ui-state/groups/dev', model);
	}

	getLongitudinal(): Observable<Record<string, unknown>> {
		return this.http.get<Record<string, unknown>>('api/ui-state/longitudinal/dev');
	}

	saveLongitudinal(model: Record<string, unknown>): Observable<unknown> {
		return this.http.post('api/ui-state/longitudinal/dev', model);
	}
}
