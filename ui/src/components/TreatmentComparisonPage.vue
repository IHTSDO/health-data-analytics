<template>
    <b-row>
        <b-col>
            <b-card
                title="Patient Cohort"
                tag="article"
                style="max-width: 20rem;"
                class="mb-2">
                <b-card-text>
                    <PatientCriteria :model="cohortCriteria"></PatientCriteria>
                </b-card-text>
            </b-card>
        </b-col>
        <b-col>
            Page: {{cohortCriteria.getForAPI()}}
        </b-col>
    </b-row>
</template>
<script lang="ts">
import { defineComponent } from 'vue'
import PatientCriteria from './PatientCriteria.vue'
import {PatientCriteriaModel} from '../model/PatientCriteriaModel'

export default defineComponent({
    name: 'TreatmentComparisonPage',
    components: {
        PatientCriteria
    },
    data() {
        return {
            cohortCriteria: new PatientCriteriaModel()
        }
    }
})
</script>
