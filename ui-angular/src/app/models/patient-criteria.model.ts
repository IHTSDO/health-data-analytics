import { ClinicalEventCriterionModel } from './clinical-event-criterion.model';

export class PatientCriteriaModel {
	dataset = '';
	gender = '';
	eventCriteria: ClinicalEventCriterionModel[] = [];
	treatment = false;

	getForAPI(): Record<string, unknown> {
		const selection: Record<string, unknown> = {
			eventCriteria: [] as unknown[],
		};
		selection['dataset'] = this.dataset;
		if (this.gender) {
			selection['gender'] = this.gender;
		}
		this.eventCriteria.forEach((criterion) => {
			if (criterion.isFilled()) {
				(selection['eventCriteria'] as unknown[]).push(criterion.getForAPI());
			}
		});
		return selection;
	}

	setAll(model: Record<string, unknown>): void {
		this.gender = (model['gender'] as string) ?? '';
		this.dataset = (model['dataset'] as string) ?? '';
		this.eventCriteria = [];
		const ec = model['eventCriteria'] as Record<string, unknown>[] | undefined;
		if (ec) {
			ec.forEach((c) => {
				const criterion = new ClinicalEventCriterionModel('', '');
				criterion.setAll(c);
				this.eventCriteria.push(criterion);
			});
		}
		if (model['treatment'] !== undefined) {
			this.treatment = !!model['treatment'];
		}
	}
}
