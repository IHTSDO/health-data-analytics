import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ReportService {
	constructor(private http: HttpClient) {}

	runReport(report: Record<string, unknown>): Observable<ReportResponse> {
		return this.http.post<ReportResponse>('api/report', report);
	}
}

export interface ReportResponse {
	groups: Array<{
		name: string;
		patientCount: number;
		groups: Array<{ name: string; patientCount: number }>;
	}>;
}
