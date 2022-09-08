<template>
    <div>
        <b-card
    title="Patient Cohort"
    tag="article"
    style="max-width: 20rem;"
    class="mb-2"
  >
    <b-card-text>
        <b-form-group label="Gender">
            <b-form-radio-group
                id="gender-group"
                v-model="gender"
                :options="genderOptions"
                name="gender-options"
            ></b-form-radio-group>
        </b-form-group>
        <b-form-group label="Condition" >
            <ConceptConstraint :constraint="condition" v-on:update:constraint="condition = $event"/>
        </b-form-group>
        Cohort Size: {{cohortSize}}
        <div hidden>{{selectionHash}}</div>
    </b-card-text>
  </b-card>
    </div>
</template>
<script>
import axios from 'axios'
import ConceptConstraint from './ConceptConstraint'

export default {
    name: 'OutcomeComparison',
    components: {
        ConceptConstraint
    },
    data() {
        return {
            gender: '',
            genderOptions: [
            {text: 'All', value: ''},
            {text: 'Female', value: 'FEMALE'},
            {text: 'Male', value: 'MALE'},
            ],
            condition: {},
            cohortSize: 0,
            numberFormat: new Intl.NumberFormat('en-US')
        }
    },
    computed: {
        selectionHash() {
            let selectionHash = this.gender + this.condition;
            this.updateCohortSize();
            return selectionHash;
        }
    },
    methods: {
        updateCohortSize() {
            let criteria = {};
            if (this.gender != '') {
                criteria.gender = this.gender;
            }
            if (this.condition.ecl) {
                criteria.encounterCriteria = [{ conceptECL: this.condition.ecl }]
            }
            console.log('updating cohort size')
            axios.post('health-analytics-api/cohorts/select', criteria)
            .then(response => {
                this.cohortSize = this.numberFormat.format(response.data.totalElements);
            })
        }
    },
    mounted() {
        this.updateCohortSize();
    }
}
</script>
<style scoped>
    h3 {
        margin: 40px 0 0;
    }
</style>
