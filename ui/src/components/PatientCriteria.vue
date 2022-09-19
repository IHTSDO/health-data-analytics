<template>
    <div>
        <b-form-group label="Gender" v-if="!hideGender">
            <b-form-radio v-model="gender" name="gender" value="">All</b-form-radio>
            <b-form-radio v-model="gender" name="gender" value="FEMALE">Female</b-form-radio>
            <b-form-radio v-model="gender" name="gender" value="MALE">Male</b-form-radio>
        </b-form-group>
        <b-form-group v-for="eventCriterion in model?.encounterCriteria" v-bind:key="eventCriterion.conceptECL" >
            <ClinicalEventCriterion :model="eventCriterion" v-on:remove="removeCriterion(eventCriterion)"/>
        </b-form-group>
        <div v-if="!hideGender">
            <div hidden>{{cohortSizeTrigger}}</div><!-- This dynamic property triggers the update of cohortSize -->
            <div>Cohort Size: {{cohortSize}}</div>
        </div>
        <AddCriteriaDropdown v-on:add-criterion="addEventCriterion"/>
    </div>
</template>
<script lang="ts">
import { defineComponent } from 'vue'
import axios from 'axios'

import {PatientCriteriaModel} from '../model/PatientCriteriaModel'
import { ClinicalEventCriterionModel } from '@/model/ClinicalEventCriterionModel';
import ClinicalEventCriterion from './ClinicalEventCriterion.vue'
import AddCriteriaDropdown from './AddCriteriaDropdown.vue'

export default defineComponent({
    name: 'PatientCriteria',
    components: {
        ClinicalEventCriterion,
        AddCriteriaDropdown
    },
    props: {
        model: PatientCriteriaModel,
        hideGender: String
    },
    mounted() {
        if (!this.model) {
            console.error("Value not set for this PatientCriteria!");
        }
    },
    updated() {
        if (this.gender != this.model?.gender) {
            this.gender = this.model.gender
        }
    },
    watch: {
        gender(newValue) {
            if (this.model) {
                this.$set(this.model, 'gender', newValue)
            }
        },
    },
    data() {
        return {
            gender: 'ALL',
            genderOptions: [
                {text: 'All', value: ''},
                {text: 'Female', value: 'FEMALE'},
                {text: 'Male', value: 'MALE'},
            ],
            numberFormat: new Intl.NumberFormat('en-US'),
            cohortSize: ""
        }
    },
    computed: {
        cohortSizeTrigger: function() {
            if (this.hideGender) {
                return 0;
            }
            const apiRequest = this.model?.getForAPI() as string;
            this.updateCohortSize(apiRequest)
            return apiRequest
        }
    },
    methods: {
        updateCohortSize(apiRequest: string) {
            axios.post('health-analytics-api/cohorts/select', apiRequest)
                .then(response => {
                    this.cohortSize = this.numberFormat.format(response.data.totalElements);
                })

        },
        addEventCriterion(display: string, eclBinding: string) {
            // Use update method for object from parent component
            if (this.model) {
                this.$set(this.model.encounterCriteria, this.model.encounterCriteria.length, new ClinicalEventCriterionModel(display, eclBinding))
            }
        },
        removeCriterion(criterion: ClinicalEventCriterionModel) {
            if (this.model) {
                const index = this.model.encounterCriteria.indexOf(criterion)
                if (index >= 0) {
                    this.$delete(this.model.encounterCriteria, index)
                }
            }
        }
    }
})

</script>
<style>

</style>
