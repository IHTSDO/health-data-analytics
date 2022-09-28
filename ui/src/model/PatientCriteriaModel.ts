import { ClinicalEventCriterionModel } from "./ClinicalEventCriterionModel"

export class PatientCriteriaModel {
    gender = ""
    encounterCriteria: Array<ClinicalEventCriterionModel> = []
    treatment = false

    getForAPI() {
        const selection: any = {
            encounterCriteria: [] as Array<{conceptECL: string}>
        };
        if (this.gender) {
            selection.gender = this.gender
        }
        this.encounterCriteria.forEach(criterion => {
            if (criterion.isFilled()) {
                selection.encounterCriteria.push(criterion.getForAPI())
            }
        });
        return selection
    }

    setAll(model: any) {
        this.gender = model.gender
        
        this.encounterCriteria.length = 0
        if (model.encounterCriteria) {
            model.encounterCriteria.forEach((c: any) => {
                const criterion = new ClinicalEventCriterionModel("", "")
                criterion.setAll(c)
                this.encounterCriteria.push(criterion)
            })
        }
    }
}
