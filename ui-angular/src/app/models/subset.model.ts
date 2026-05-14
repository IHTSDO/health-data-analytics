export class SubsetModel {
	id = '';
	name = '';
	description = '';
	ecl = '';

	constructor(id: string, name: string) {
		this.id = id;
		this.name = name;
	}

	clone(): SubsetModel {
		const n = new SubsetModel(this.id, this.name);
		n.description = this.description;
		n.ecl = this.ecl;
		return n;
	}
}
