import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CorrelationDiscoveryResponse {
	nodes: Array<{ label: string; conceptId: string }>;
}

@Injectable({ providedIn: 'root' })
export class CorrelationService {
	constructor(private http: HttpClient) {}

	discover(body: Record<string, unknown>): Observable<CorrelationDiscoveryResponse> {
		return this.http.post<CorrelationDiscoveryResponse>('api/correlation-discovery-report', body);
	}
}
