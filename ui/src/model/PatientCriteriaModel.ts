import { ClinicalEventCriterionModel } from "./ClinicalEventCriterionModel"

export class PatientCriteriaModel {
    dataset = ""
    gender = ""
    eventCriteria: Array<ClinicalEventCriterionModel> = []
    treatment = false

    getForAPI() {
        const selection: any = {
            eventCriteria: [] as Array<{conceptECL: string}>
        };
        selection.dataset = this.dataset
        if (this.gender) {
            selection.gender = this.gender
        }
        this.eventCriteria.forEach(criterion => {
            if (criterion.isFilled()) {
                selection.eventCriteria.push(criterion.getForAPI())
            }
        });
        return selection
    }

    setAll(model: any) {
        this.gender = model.gender
        
        this.eventCriteria.length = 0
        if (model.eventCriteria) {
            model.eventCriteria.forEach((c: any) => {
                const criterion = new ClinicalEventCriterionModel("", "")
                criterion.setAll(c)
                this.eventCriteria.push(criterion)
            })
        }
    }
}
