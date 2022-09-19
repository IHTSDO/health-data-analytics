export class ClinicalEventCriterionModel {
    title!: string
    conceptECL!: string
    display!: string

    eclBinding!: string
    color!: string
    initial!: string


    constructor(title: string, eclBinding: string) {
        this.title = title
        this.eclBinding = eclBinding
    }

    isFilled() {
        if (this.conceptECL) {
            return true
        }
        return false
    }

    getForAPI() {
        return {conceptECL: this.conceptECL}
    }

    setAll(model: any) {
        this.title = model.title
        this.conceptECL = model.conceptECL
        this.display = model.display
        this.eclBinding = model.eclBinding
        this.color = model.color
    }

}
