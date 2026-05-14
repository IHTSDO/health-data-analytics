import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ConceptItem {
	code: string;
	display: string;
}

@Injectable({ providedIn: 'root' })
export class ConceptSearchService {
	constructor(private http: HttpClient) {}

	searchConcepts(prefix: string, eclBinding: string): Observable<ConceptItem[]> {
		return this.http.get<ConceptItem[]>(
			`api/concepts?prefix=${encodeURIComponent(prefix)}&ecl=${encodeURIComponent(eclBinding)}&limit=10`
		);
	}

	searchSubsetsPrefix(prefix: string): Observable<{ content: { id: string; name: string; ecl: string }[] }> {
		return this.http.get<{ content: { id: string; name: string; ecl: string }[] }>(
			`api/subsets?prefix=${encodeURIComponent(prefix)}`
		);
	}

	listDatasets(): Observable<string[]> {
		return this.http.get<string[]>('api/datasets');
	}
}
