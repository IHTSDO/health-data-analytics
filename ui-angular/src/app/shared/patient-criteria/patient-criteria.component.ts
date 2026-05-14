import {
	Component,
	Input,
	OnChanges,
	OnDestroy,
	OnInit,
	SimpleChanges,
} from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { PatientCriteriaModel } from '../../models/patient-criteria.model';
import { ClinicalEventCriterionModel } from '../../models/clinical-event-criterion.model';
import { ConceptSearchService } from '../../services/concept-search.service';
import { CohortService } from '../../services/cohort.service';

@Component({
	selector: 'app-patient-criteria',
	templateUrl: './patient-criteria.component.html',
	styleUrls: ['./patient-criteria.component.scss'],
	standalone: false,
})
export class PatientCriteriaComponent implements OnInit, OnDestroy, OnChanges {
	@Input() model!: PatientCriteriaModel;
	@Input() hideGender = false;
	@Input() hideSize = false;

	datasets: string[] = [];
	cohortSize = '';
	groupId = '';
	private readonly numberFormat = new Intl.NumberFormat('en-US');
	private readonly refresh$ = new Subject<void>();
	private sub?: Subscription;

	constructor(
		private concepts: ConceptSearchService,
		private cohort: CohortService
	) {}

	ngOnInit(): void {
		this.groupId = 'pc-' + Math.random().toString(36).slice(2);
		this.concepts.listDatasets().subscribe((d) => (this.datasets = d));
		this.sub = this.refresh$.pipe(debounceTime(200)).subscribe(() => this.updateCohortSize());
		this.refresh$.next();
	}

	ngOnChanges(changes: SimpleChanges): void {
		if (changes['model']?.currentValue && !this.hideGender) {
			this.refresh$.next();
		}
	}

	ngOnDestroy(): void {
		this.sub?.unsubscribe();
	}

	get genderValue(): string {
		return this.model?.gender ?? '';
	}
	set genderValue(v: string) {
		if (this.model) {
			this.model.gender = v;
			this.refresh$.next();
		}
	}

	get datasetValue(): string {
		return this.model?.dataset ?? '';
	}
	set datasetValue(v: string) {
		if (this.model) {
			this.model.dataset = v;
			this.refresh$.next();
		}
	}

	addEventCriterion(display: string, eclBinding: string): void {
		if (this.model) {
			this.model.eventCriteria.push(new ClinicalEventCriterionModel(display, eclBinding));
			this.refresh$.next();
		}
	}

	removeCriterion(criterion: ClinicalEventCriterionModel): void {
		if (!this.model) {
			return;
		}
		const index = this.model.eventCriteria.indexOf(criterion);
		if (index >= 0) {
			this.model.eventCriteria.splice(index, 1);
			this.refresh$.next();
		}
	}

	onCriterionChanged(): void {
		this.refresh$.next();
	}

	private updateCohortSize(): void {
		if (this.hideGender || !this.model) {
			return;
		}
		this.cohort.select(this.model.getForAPI()).subscribe({
			next: (response) => {
				this.cohortSize = this.numberFormat.format(response.totalElements);
			},
		});
	}
}
