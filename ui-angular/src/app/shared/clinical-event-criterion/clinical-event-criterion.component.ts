import {
	Component,
	EventEmitter,
	Input,
	OnChanges,
	OnDestroy,
	OnInit,
	Output,
	SimpleChanges,
} from '@angular/core';
import { Subject, Subscription, firstValueFrom } from 'rxjs';
import { debounceTime, filter } from 'rxjs/operators';
import { ClinicalEventCriterionModel } from '../../models/clinical-event-criterion.model';
import { ConceptSearchService } from '../../services/concept-search.service';

@Component({
	selector: 'app-clinical-event-criterion',
	templateUrl: './clinical-event-criterion.component.html',
	styleUrls: ['./clinical-event-criterion.component.scss'],
	standalone: false,
})
export class ClinicalEventCriterionComponent implements OnInit, OnDestroy, OnChanges {
	@Input() model!: ClinicalEventCriterionModel;
	@Output() remove = new EventEmitter<void>();
	@Output() changed = new EventEmitter<void>();

	searchInput = '';
	searchResults: DropdownItem[] = [];
	showResultsDropdown = false;
	private itemJustSelected = false;
	private readonly search$ = new Subject<string>();
	private sub?: Subscription;

	constructor(private concepts: ConceptSearchService) {}

	ngOnChanges(changes: SimpleChanges): void {
		if (changes['model']?.currentValue && this.model?.conceptECL) {
			this.syncDisplayFromModel();
		}
	}

	ngOnInit(): void {
		if (this.model?.initial) {
			this.fhirSearch(this.model.initial);
		}
		if (!this.model?.eclBinding) {
			console.warn('No ECL binding defined.');
		}
		this.sub = this.search$
			.pipe(debounceTime(500), filter((input) => input.length > 2))
			.subscribe((input) => this.fhirSearch(input));
	}

	ngOnDestroy(): void {
		this.sub?.unsubscribe();
	}

	syncDisplayFromModel(): void {
		if (this.model?.conceptECL) {
			this.itemJustSelected = true;
			this.searchInput = this.model.display ?? '';
		}
	}

	get boxStyle(): Record<string, string> {
		if (this.model?.color) {
			return { 'background-color': this.model.color, color: 'white' };
		}
		return {};
	}

	get tooltipText(): string {
		return `${this.model?.conceptECL ?? ''}${this.model?.historyECL ?? ''}`;
	}

	onSearchInput(value: string): void {
		this.searchInput = value;
		if (this.itemJustSelected) {
			this.itemJustSelected = false;
			return;
		}
		this.search$.next(value);
	}

	selectResult(item: DropdownItem): void {
		if (this.model) {
			this.model.display = item.display;
			this.model.conceptECL = item.getEcl();
		}
		this.searchResults = [];
		this.showResultsDropdown = false;
		this.changed.emit();
	}

	private async fhirSearch(input: string): Promise<void> {
		const subsetsResp = await firstValueFrom(this.concepts.searchSubsetsPrefix(input));
		const matchingSubsets = subsetsResp?.content ?? [];

		this.concepts.searchConcepts(input, this.model?.eclBinding ?? '').subscribe({
			next: (response) => {
				if (response.length === 1 && matchingSubsets.length === 0) {
					const row = response[0];
					this.selectResult(new DropdownItem(row.code, row.display));
					return;
				}
				this.searchResults = [];
				matchingSubsets.forEach((element) => {
					this.searchResults.push(
						new DropdownItem(element.id, `${element.name} (subset)`, element.ecl)
					);
				});
				response.forEach((element) => {
					this.searchResults.push(new DropdownItem(element.code, element.display));
				});
				this.showResultsDropdown = this.searchResults.length > 0;
			},
		});
	}
}

export class DropdownItem {
	code: string;
	display: string;
	ecl?: string;
	subset = false;

	constructor(code: string, display: string, ecl?: string) {
		this.code = code;
		this.display = display;
		this.ecl = ecl;
		this.subset = !!ecl;
	}

	getCodeTerm(): string {
		if (this.subset) {
			return `Subset: ${this.display}`;
		}
		return `${this.code} |${this.display}|`;
	}

	getEcl(): string {
		if (this.subset) {
			return this.ecl as string;
		}
		return `<< ${this.getCodeTerm()}`;
	}
}
