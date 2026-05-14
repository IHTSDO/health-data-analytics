import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class CohortService {
	constructor(private http: HttpClient) {}

	select(body: Record<string, unknown>): Observable<{ totalElements: number }> {
		return this.http.post<{ totalElements: number }>('api/cohorts/select', body);
	}
}
