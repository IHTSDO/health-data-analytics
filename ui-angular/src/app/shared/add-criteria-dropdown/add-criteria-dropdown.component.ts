import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
	selector: 'app-add-criteria-dropdown',
	templateUrl: './add-criteria-dropdown.component.html',
	styleUrls: ['./add-criteria-dropdown.component.scss'],
	standalone: false,
})
export class AddCriteriaDropdownComponent {
	@Input() treatment = false;
	@Input() label = 'Add Requirement';
	@Output() addCriterion = new EventEmitter<{ display: string; eclBinding: string }>();

	emit(display: string, eclBinding: string): void {
		this.addCriterion.emit({ display, eclBinding });
	}
}
