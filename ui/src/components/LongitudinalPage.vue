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
    </b-row>
</template>
<script>
import PatientCriteria from './PatientCriteria'

export default {
    name: 'LongitudinalPage',
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
