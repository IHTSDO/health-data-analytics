export class ClinicalEventCriterionModel {
    title!: string
    conceptECL!: string
    display!: string
    withinDaysAfterPreviouslyMatchedEncounter!: number

    eclBinding!: string
    color!: string
    initial!: string
    historyECL!: string

    constructor(title: string, eclBinding: string, initial?: string) {
        this.title = title
        this.eclBinding = eclBinding
        if (initial) {
            this.initial = initial
        }
        this.historyECL = ''
    }

    isFilled() {
        if (this.conceptECL) {
            return true
        }
        return false
    }

    getForAPI(startDate?: Date, endDate?: Date) {
        const apiFormat = { conceptECL: this.conceptECL + this.historyECL } as any
        if (startDate) {
            apiFormat.minDate = startDate
        }
        if (endDate) {
            apiFormat.maxDate = endDate
        }
        if (typeof this.withinDaysAfterPreviouslyMatchedEncounter != 'undefined') {
            apiFormat.withinDaysAfterPreviouslyMatchedEncounter = this.withinDaysAfterPreviouslyMatchedEncounter
        }
        return apiFormat
    }

    setAll(model: any) {
        this.title = model.title
        this.conceptECL = model.conceptECL
        this.display = model.display
        this.eclBinding = model.eclBinding
        this.color = model.color
    }

}
