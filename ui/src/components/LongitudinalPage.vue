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
                            <div hidden>{{timepoints}}</div>
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
            <b-button v-on:click="run">Run</b-button>
            <!-- <b-button v-on:click="save">Save</b-button> -->
            <!-- <b-button v-on:click="load">Load</b-button> -->
        </b-col>
        <b-col>
            <ReportChartLongitudinal ref="chart" ></ReportChartLongitudinal>
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
import ReportChartLongitudinal from './ReportChartLongitudinal.vue';

export default defineComponent({
    name: 'LongitudinalPage',
    components: {
        ClinicalEventCriterion,
        PatientCriteria,
        AddCriteriaDropdown,
        ReportChartLongitudinal
    },
    data() {
        return {
            loaded: false,
            cohortCriteria: new PatientCriteriaModel(),
            startDate: "2000-01-01",
            endDate: "2010-12-31",
            granularity: "YEAR",
            groups: [
                {
                    name: "Everyone",
                    criteria: new PatientCriteriaModel()
                }
            ],
            outcomes: new Array<ClinicalEventCriterionModel>(),
            outcomesIncludeHistory: false,
            eclHistory: " {{ +HISTORY }}",
            cohortSize: "0",
            numberFormat: new Intl.NumberFormat('en-US'),

            hideChart: false
        }
    },
    watch: {
        outcomesIncludeHistory() {
            console.log("outcomesIncludeHistory");
            
            this.outcomes.forEach(outcome => {
                var postfix = this.eclHistory
                if (!this.outcomesIncludeHistory) {
                    postfix = ""
                }
                const newValue = outcome.conceptECL.replaceAll(this.eclHistory, "") + postfix
                console.log("newValue ", newValue);
                
                this.$set(this.outcomes[this.outcomes.indexOf(outcome)], 'conceptECL', newValue)
            })
        }
    },
    mounted() {
        this.load()
        this.outcomes.push(new ClinicalEventCriterionModel('Clinical Finding', '<404684003', '195967001'))// Asthma
        this.outcomes.push(new ClinicalEventCriterionModel('Clinical Finding', '<404684003', '52448006'))// dementia
        this.outcomes.push(new ClinicalEventCriterionModel('Clinical Finding', '<404684003', '13645005'))// copd
        this.outcomes.push(new ClinicalEventCriterionModel('Clinical Finding', '<404684003', '64859006'))// osteoporosis
        this.outcomes.push(new ClinicalEventCriterionModel('Clinical Finding', '<404684003', '46635009'))// diabetes 1
        this.outcomes.push(new ClinicalEventCriterionModel('Clinical Finding', '<404684003', '44054006'))// diabetes 2
    },
    computed: {
        timepoints() {
            console.log("building timepoints");
            
            const points = [] as Array<Date>
            // int year

            let current = this.parseDate(this.startDate) as Date
            let end = this.parseDate(this.endDate) as Date
            if (current && end) {
                let i = 0
                const a = current.getTime()
                const b = end.getTime()
                while (current.getTime() <= end.getTime() && i++ < 1000) {
                    points.push(new Date(current.getTime()))
                    if (this.granularity == "YEAR") {
                        current.setUTCFullYear(current.getUTCFullYear() + 1)
                    } else if (this.granularity == "MONTH") {
                        current.setUTCMonth(current.getUTCMonth() + 1)
                    } else if (this.granularity == "DAY") {
                        current.setUTCDate(current.getUTCDate() + 1)
                    } else if (this.granularity == "HOUR") {
                        current.setUTCHours(current.getUTCHours() + 1)
                    }
                }
            }
            return points
        }
    },
    methods: {
        parseDate(input: string) {
            const regex = new RegExp('[0-9]{4}-[0-9]{2}-[0-9]{2}');
            if (input && regex.test(input)) {
                const parts = input.split('-')
                // 2001-01-01T00:00:00.000Z
                
                return new Date(parts[0], Number.parseInt(parts[1]) -1, parts[2])
            }
            return null
        },
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
                // console.log('updating cohort size')
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
        async run() {
            const reportRequests = [] as Array<any>
            let startTimepoint: Date

            this.timepoints.forEach(endTimepoint => {
                if (startTimepoint) {
                    reportRequests.push(this.getReportRequest(startTimepoint, endTimepoint, startTimepoint.getUTCFullYear() + ""))
                }
                startTimepoint = endTimepoint
            })
            // console.log("this.timepoints", this.timepoints);
            // console.log("reportRequests", reportRequests);
            
            this.$refs.chart.fetchReports(reportRequests)
        },
        getReportRequest: function(startDate: Date, endDate: Date, label: string) {
            const report = {} as any;
            report.name = label
            report.criteria = this.cohortCriteria.getForAPI()

            const patientGroups = new Array<unknown>();
            const groupCriteria = {} as any;
            patientGroups.push(groupCriteria)

            const outcomesRequest = new Array<unknown>();
            const colors = new Array<string>();
            this.outcomes.forEach(outcome => {
                if (outcome.isFilled()) {
                    colors.push(outcome.color)                    
                    outcomesRequest.push({
                        name: outcome.display,
                        criteria: {
                            encounterCriteria: [outcome.getForAPI(startDate, endDate)]
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