
export class SubsetModel {
    id = ""
    name = ""
    description = ""
    ecl = ""

    constructor(id: string, name: string) {
        this.id = id
        this.name = name
    }

    clone() {
        const newModel = new SubsetModel(this.id, this.name)
        newModel.description = this.description
        newModel.ecl = this.ecl
        return newModel
    }
}
