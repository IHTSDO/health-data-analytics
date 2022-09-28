<template>
    <b-row>
        <b-col cols="4">
            <div>
                <b-card
                    title="Patient Cohort"
                    tag="article"
                    style="max-width: 30rem;"
                    class="mb-2">
                    <b-card-text>
                        <PatientCriteria :model="cohortCriteria"></PatientCriteria>
                        {{cohortCriteria.gender}}
                    </b-card-text>
                </b-card>
                <b-card
                    title="Time Period:"
                    tag="article"
                    style="max-width: 30rem;"
                    class="mb-2">
                    <b-card-text>
                        <div>
                            <b-form-group label="Start date">
                                <b-input-group class="mb-3">
                                    <b-form-input
                                        id="example-input"
                                        v-model="startDate"
                                        type="text"
                                        placeholder="YYYY-MM-DD"
                                        autocomplete="off"
                                    ></b-form-input>
                                    <b-input-group-append>
                                        <b-form-datepicker
                                        v-model="startDate"
                                        button-only
                                        right
                                        locale="en-US"
                                        aria-controls="example-input"
                                        ></b-form-datepicker>
                                    </b-input-group-append>
                                </b-input-group>
                            </b-form-group>                            
                            <b-form-group label="End date">
                                <b-input-group class="mb-3">
                                    <b-form-input
                                        id="example-input"
                                        v-model="endDate"
                                        type="text"
                                        placeholder="YYYY-MM-DD"
                                        autocomplete="off"
                                    ></b-form-input>
                                    <b-input-group-append>
                                        <b-form-datepicker
                                        v-model="endDate"
                                        button-only
                                        right
                                        locale="en-US"
                                        aria-controls="example-input"
                                        ></b-form-datepicker>
                                    </b-input-group-append>
                                </b-input-group>
                            </b-form-group>
                            <b-form-group label="Granularity">
                                <b-form-radio v-model="granularity" name="granularity" value="YEAR">Year</b-form-radio>
                                <b-form-radio v-model="granularity" name="granularity" value="MONTH">Month</b-form-radio>
                                <b-form-radio v-model="granularity" name="granularity" value="DAY">Day</b-form-radio>
                                <b-form-radio v-model="granularity" name="granularity" value="HOUR">Hour</b-form-radio>
                            </b-form-group>
                        </div>
                    </b-card-text>
                </b-card>
                <b-card
                    title="Outcomes to Measure:"
                    tag="article"
                    style="max-width: 30rem;"
                    class="mb-2">
                    <b-card-text>
                        <b-form-group v-for="outcome in outcomes" v-bind:key="outcome.conceptECL">
                            <ClinicalEventCriterion :model="outcome"/>
                        </b-form-group>
                        <AddCriteriaDropdown label="Add Outcome" v-on:add-criterion="addOutcome"/>
                        <b-check v-model="outcomesIncludeHistory">Include historic concepts</b-check>
                    </b-card-text>
                </b-card>
            </div>
            <b-button v-on:click="save">Save</b-button>
            <b-button v-on:click="load">Load</b-button>
        </b-col>
        <b-col>
            <div hidden>{{conditionsTrigger}}</div>
            <ReportChart ref="chart" :series="series" ></ReportChart>
        </b-col>
    </b-row>
</template>
<script lang="ts">
import { defineComponent } from 'vue'
import axios from 'axios'
import debounce from 'lodash.debounce'
import { plainToInstance } from 'class-transformer';

import ClinicalEventCriterion from './ClinicalEventCriterion.vue'
import { ClinicalEventCriterionModel } from './../model/ClinicalEventCriterionModel'
import PatientCriteria from './PatientCriteria.vue'
import { PatientCriteriaModel } from '@/model/PatientCriteriaModel'
import AddCriteriaDropdown from './AddCriteriaDropdown.vue'
import ReportChart from './ReportChart.vue';

export default defineComponent({
    name: 'LongitudinalPage',
    components: {
        ClinicalEventCriterion,
        PatientCriteria,
        AddCriteriaDropdown,
        ReportChart
    },
    data() {
        return {
            loaded: false,
            cohortCriteria: new PatientCriteriaModel(),
            startDate: null,
            endDate: null,
            granularity: "YEAR",
            groups: [
                {
                    name: "Everyone",
                    criteria: new PatientCriteriaModel()
                },
                {
                    name: "",
                    criteria: new PatientCriteriaModel()
                }
            ],
            outcomes: new Array<ClinicalEventCriterionModel>(),
            outcomesIncludeHistory: false,
            eclHistory: " {{ +HISTORY }}",
            cohortSize: "0",
            numberFormat: new Intl.NumberFormat('en-US'),

            // apex
            series: [{data: []}],
        }
    },
    watch: {
        outcomesIncludeHistory() {
            this.outcomes.forEach(outcome => {
                var postfix = this.eclHistory
                if (!this.outcomesIncludeHistory) {
                    postfix = ""
                }
                outcome.conceptECL = outcome.conceptECL.replaceAll(this.eclHistory, "") + postfix
            })
        }
    },
    mounted() {
        this.load()
    },
    computed: {
        // Used to monitor changes in selection criteria and trigger API interactions
        patientCriteriaTrigger() {
            const selectionHash: any = this.cohortCriteria.getForAPI()
            // this.save()
            this.updateCohortSize()
            return selectionHash;
        },
        conditionsTrigger() {
            const reportRequest = this.getReportRequest()
            this.updateOutcomes(reportRequest);
            return reportRequest;
        }
    },
    methods: {
        load() {
            axios.get('api/ui-state/longitudinal/dev')
            .then(response => {
                // console.log("Load");
                const model = response.data
                if (model && model.cohortCriteria) {
                    this.cohortCriteria.setAll(model.cohortCriteria)
                    // console.log("getForApi after plainToClass", this.cohortCriteria.getForAPI());

                    this.groups.length = 0
                    model.groups.forEach((group: any) => {
                        const gC = new PatientCriteriaModel()
                        if (group.criteria) {
                            gC.setAll(group.criteria)
                        }
                        this.groups.push({
                            name: group.name,
                            criteria: gC
                        })
                    })

                    this.outcomes = plainToInstance(ClinicalEventCriterionModel, model.outcomes)
                    this.outcomesIncludeHistory = model.outcomesIncludeHistory
                    this.loaded = true
                } else {
                    this.cohortCriteria.encounterCriteria.push()
                    this.loaded = true
                }
            })
        },
        save() {
            if (!this.loaded) {
            // if (10 * 10 == 100 || !this.loaded) {
                return
            }
            const model = {
                cohortCriteria: this.cohortCriteria,
                groups: this.groups,
                outcomes: this.outcomes,
                outcomesIncludeHistory: this.outcomesIncludeHistory
            }
            // console.log(model);
            axios.post('api/ui-state/longitudinal/dev', model);
            console.log("saved:", this.cohortCriteria.gender);
        },
        addOutcome(display: string, eclBinding: string) {
            this.outcomes.push(new ClinicalEventCriterionModel(display, eclBinding))
        },
        addGroup() {
            this.groups.push({name: "", criteria: new PatientCriteriaModel()})
        },
        updateCohortSize: function() {
            // eslint-disable-next-line
            const context = this;
            debounce(function() {
                console.log('updating cohort size')
                axios.post('api/cohorts/select', context.cohortCriteria.getForAPI())
                    .then(response => {
                        context.cohortSize = context.numberFormat.format(response.data.totalElements);
                    })
            }, 100)
        },
        updateOutcomes: function(report: any) {
            if (report.groups && report.groups.length == 2 && report.groups[1].length) {
                this.$refs.chart.fetchReport(report)
            }
        },
        getReportRequest: function() {
            const report = {} as any;
            report.criteria = this.cohortCriteria.getForAPI()

            const patientGroups = new Array<unknown>();
            this.groups.forEach(group => {
                const groupCriteria = {} as any;
                patientGroups.push(groupCriteria)
                groupCriteria.name = group.name;
                groupCriteria.criteria = group.criteria.getForAPI()
            })

            const outcomesRequest = new Array<unknown>();
            const colors = new Array<string>();
            this.outcomes.forEach(outcome => {
                if (outcome.isFilled()) {
                    colors.push(outcome.color)
                    outcomesRequest.push({
                        criteria: {
                            encounterCriteria: [outcome.getForAPI()]
                        }
                    })
                }
            })
            report.groups = [patientGroups, outcomesRequest];
            report.colors = colors
            return report;
        },
    }
})
</script>
<style scoped>
h3 {
    margin: 40px 0 0;
}

.patient-group {
    border: 1px solid lightgray;
    margin: 10px;
    padding: 5px;
}
</style>
<style>
legend {
    text-align: left;
    font-weight: bold;
}
</style>