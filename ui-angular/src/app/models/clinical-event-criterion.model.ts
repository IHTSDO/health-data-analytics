export class ClinicalEventCriterionModel {
	title!: string;
	conceptECL!: string;
	display!: string;
	withinDaysAfterPreviouslyMatchedEvent?: number;

	eclBinding!: string;
	color!: string;
	initial!: string;
	historyECL!: string;

	constructor(title: string, eclBinding: string, initial?: string) {
		this.title = title;
		this.eclBinding = eclBinding;
		if (initial) {
			this.initial = initial;
		}
		this.historyECL = '';
	}

	isFilled(): boolean {
		return !!this.conceptECL;
	}

	getForAPI(startDate?: Date, endDate?: Date): Record<string, unknown> {
		const apiFormat: Record<string, unknown> = {
			conceptECL: this.conceptECL + this.historyECL,
		};
		if (startDate) {
			apiFormat['minDate'] = startDate;
		}
		if (endDate) {
			apiFormat['maxDate'] = endDate;
		}
		if (typeof this.withinDaysAfterPreviouslyMatchedEvent !== 'undefined') {
			apiFormat['withinDaysAfterPreviouslyMatchedEvent'] =
				this.withinDaysAfterPreviouslyMatchedEvent;
		}
		return apiFormat;
	}

	setAll(model: Record<string, unknown>): void {
		this.title = model['title'] as string;
		this.conceptECL = model['conceptECL'] as string;
		this.display = model['display'] as string;
		this.eclBinding = model['eclBinding'] as string;
		this.color = model['color'] as string;
		if (model['historyECL'] !== undefined) {
			this.historyECL = model['historyECL'] as string;
		}
	}

	static fromPlain(o: Record<string, unknown>): ClinicalEventCriterionModel {
		const m = new ClinicalEventCriterionModel(
			(o['title'] as string) ?? '',
			(o['eclBinding'] as string) ?? ''
		);
		m.setAll(o);
		m.initial = (o['initial'] as string) ?? '';
		m.withinDaysAfterPreviouslyMatchedEvent = o['withinDaysAfterPreviouslyMatchedEvent'] as
			| number
			| undefined;
		return m;
	}
}
