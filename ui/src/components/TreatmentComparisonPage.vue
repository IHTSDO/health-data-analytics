<template>
    <b-row>
        <b-col>
            <b-card
                title="Patient Cohort"
                tag="article"
                style="max-width: 20rem;"
                class="mb-2">
                <b-card-text>
                    <PatientCriteria v-model="cohortCriteria"></PatientCriteria>
                </b-card-text>
            </b-card>
        </b-col>
        <b-col>
            {{cohortCriteria}}
        </b-col>
        <b-col>
            {{cohortCriteria.selected}}
        </b-col>
    </b-row>
</template>
<script>
import PatientCriteria from './PatientCriteria'

export default {
    name: 'TreatmentComparisonPage',
    components: {
        PatientCriteria
    },
    data() {
        return {
            cohortCriteria: {}
        }
    }
}
</script>
