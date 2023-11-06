<template>
    <div>
        <b-form-group label="Patient Dataset" v-if="!hideGender">
            <b-form-select v-model="dataset" :options="datasets" size="sm"></b-form-select>
        </b-form-group>
        <b-form-group label="Gender" v-if="!hideGender">
            <b-form-radio v-model="gender" name="gender" value="">All</b-form-radio>
            <b-form-radio v-model="gender" name="gender" value="FEMALE">Female</b-form-radio>
            <b-form-radio v-model="gender" name="gender" value="MALE">Male</b-form-radio>
        </b-form-group>
        <b-form-group v-for="eventCriterion in model?.eventCriteria" v-bind:key="eventCriterion.conceptECL" >
            <ClinicalEventCriterion :model="eventCriterion" v-on:remove="removeCriterion(eventCriterion)"/>
        </b-form-group>
        <div v-if="!(hideGender || hideSize)">
            <div hidden>{{cohortSizeTrigger}}</div><!-- This dynamic property triggers the update of cohortSize -->
            <div>Cohort Size: {{cohortSize}}</div>
        </div>
        <AddCriteriaDropdown :treatment="model?.treatment" v-on:add-criterion="addEventCriterion"></AddCriteriaDropdown>
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
        hideGender: String,
        hideSize: Boolean,
    },
    mounted() {
        if (!this.model) {
            console.error("Value not set for this PatientCriteria!");
        } else {
            axios.get('api/datasets')
                .then(response => {
                    this.datasets = response.data;
                })
        }
    },
    updated() {
        if (this.model && this.gender != this.model?.gender) {
            this.gender = this.model.gender
        }
    },
    watch: {
        gender(newValue) {
            if (this.model) {
                this.$set(this.model, 'gender', newValue)
            }
        },
        dataset(newValue) {
            if (this.model) {
                this.$set(this.model, 'dataset', newValue)
            }
        },
    },
    data() {
        return {
            dataset: '',
            gender: '',
            genderOptions: [
                {text: 'All', value: ''},
                {text: 'Female', value: 'FEMALE'},
                {text: 'Male', value: 'MALE'},
            ],
            datasets: [
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
            axios.post('api/cohorts/select', apiRequest)
                .then(response => {
                    this.cohortSize = this.numberFormat.format(response.data.totalElements);
                })

        },
        addEventCriterion(display: string, eclBinding: string) {
            // Use update method for object from parent component
            if (this.model) {
                this.$set(this.model.eventCriteria, this.model.eventCriteria.length, new ClinicalEventCriterionModel(display, eclBinding))
            }
        },
        removeCriterion(criterion: ClinicalEventCriterionModel) {
            if (this.model) {
                const index = this.model.eventCriteria.indexOf(criterion)
                if (index >= 0) {
                    this.$delete(this.model.eventCriteria, index)
                }
            }
        }
    }
})

</script>
<style>
.custom-control.custom-radio {
    display: inline-block;
    margin-right: 10px
}
</style>
