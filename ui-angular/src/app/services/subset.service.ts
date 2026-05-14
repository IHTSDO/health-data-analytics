import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SubsetModel } from '../models/subset.model';

interface PageContent<T> {
	content: T[];
}

@Injectable({ providedIn: 'root' })
export class SubsetService {
	constructor(private http: HttpClient) {}

	list(): Observable<PageContent<Record<string, unknown>>> {
		return this.http.get<PageContent<Record<string, unknown>>>('api/subsets');
	}

	get(id: string): Observable<Record<string, unknown>> {
		return this.http.get<Record<string, unknown>>(`api/subsets/${encodeURIComponent(id)}`);
	}

	put(id: string, subset: SubsetModel): Observable<Record<string, unknown>> {
		return this.http.put<Record<string, unknown>>(`api/subsets/${encodeURIComponent(id)}`, subset);
	}

	delete(id: string): Observable<Record<string, unknown>> {
		return this.http.delete<Record<string, unknown>>(`api/subsets/${encodeURIComponent(id)}`);
	}

	findConcepts(ecl: string): Observable<{
		items: { conceptId: string; pt: { term: string } }[];
		total: number;
	}> {
		return this.http.get<{
			items: { conceptId: string; pt: { term: string } }[];
			total: number;
		}>(`/api/snowstorm/MAIN/concepts?ecl=${encodeURIComponent(ecl)}`);
	}
}
