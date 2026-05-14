import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { SubsetModel } from '../../models/subset.model';
import { SubsetService } from '../../services/subset.service';

@Component({
	selector: 'app-subsets',
	templateUrl: './subsets.component.html',
	styleUrls: ['./subsets.component.scss'],
	standalone: false,
})
export class SubsetsComponent implements OnInit, OnDestroy {
	subsets: SubsetModel[] = [];
	selectedSubset = new SubsetModel('', '');
	newEcl = '';
	matchingConcepts: { id: string; PreferredTerm: string }[] = [];
	matchingConceptsCount = 0;
	private routeSub?: Subscription;

	constructor(
		private subsetApi: SubsetService,
		private route: ActivatedRoute,
		private router: Router
	) {}

	ngOnInit(): void {
		this.loadSubsets();
		this.routeSub = this.route.params.subscribe(() => this.selectUsingUrl());
	}

	ngOnDestroy(): void {
		this.routeSub?.unsubscribe();
	}

	eclTextareaChange(value: string): void {
		this.newEcl = value;
		this.matchingConcepts = [];
	}

	loadSubsets(): void {
		this.subsetApi.list().subscribe((page) => {
			const items = page.content ?? [];
			this.subsets = items.map((raw) => this.createSubsetFromApi(raw));
			this.selectUsingUrl();
		});
	}

	selectUsingUrl(): void {
		const id = this.route.snapshot.paramMap.get('id');
		if (id) {
			this.loadSubset(id);
		} else {
			this.selectedSubset = new SubsetModel('', '');
			this.newEcl = '';
			this.matchingConcepts = [];
		}
	}

	loadSubset(id: string): void {
		this.subsetApi.get(id).subscribe((raw) => {
			this.selectedSubset = this.createSubsetFromApi(raw);
			this.newEcl = this.selectedSubset.ecl ?? '';
			this.matchingConcepts = [];
		});
	}

	addSubset(): void {
		const newSubset = new SubsetModel(this.uuidv4(), 'new');
		newSubset.ecl = '<< 404684003 |Clinical finding (finding)|';
		this.subsetApi.put(newSubset.id, newSubset).subscribe(() => {
			this.subsets.push(newSubset);
			void this.router.navigate(['/subsets', newSubset.id]);
		});
	}

	saveSubset(): void {
		const toSave = this.selectedSubset.clone();
		toSave.ecl = this.newEcl;
		this.subsetApi.put(toSave.id, toSave).subscribe(() => {
			this.loadSubsets();
			this.loadSubset(toSave.id);
		});
	}

	deleteSubset(): void {
		const id = this.selectedSubset.id;
		this.subsetApi.delete(id).subscribe(() => {
			this.loadSubsets();
			void this.router.navigate(['/subsets']);
		});
	}

	findConcepts(): void {
		this.matchingConcepts = [];
		let ecl = this.selectedSubset.ecl;
		if (this.newEcl) {
			ecl = this.newEcl;
		}
		this.subsetApi.findConcepts(ecl).subscribe((response) => {
			response.items.forEach((concept) => {
				this.matchingConcepts.push({
					id: concept.conceptId,
					PreferredTerm: concept.pt.term,
				});
			});
			this.matchingConceptsCount = response.total;
		});
	}

	createSubsetFromApi(raw: Record<string, unknown>): SubsetModel {
		const subset = new SubsetModel(raw['id'] as string, raw['name'] as string);
		subset.description = (raw['description'] as string) ?? '';
		subset.ecl = (raw['ecl'] as string) ?? '';
		return subset;
	}

	private uuidv4(): string {
		return crypto.randomUUID();
	}
}
