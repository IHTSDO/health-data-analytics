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
                    title="Outcomes to Measure:"
                    tag="article"
                    style="max-width: 30rem;"
                    class="mb-2">
                    <b-card-text>
                        <b-form-group v-for="(outcome, index) in outcomes" v-bind:key="outcome.conceptECL">
                            <ClinicalEventCriterion :model="outcome" v-on:remove="outcomes.splice(index, 1)"/>
                        </b-form-group>
                        <AddCriteriaDropdown label="Add Outcome" v-on:add-criterion="addOutcome"/>
                    </b-card-text>
                </b-card>
                <b-card
                    title="For Patient Groups:"
                    tag="article"
                    style="max-width: 30rem;"
                    class="mb-2">
                    <b-card-text>
                        <div class="patient-group" style="animation: pulse 1.5s infinite;" v-if="this.discovering">
                            Analyzing
                        </div>
                        <div v-for="(group, index) in groups" v-bind:key="group.name" class="patient-group">
                            <b-row>
                                <b-col>
                                    <b-button class="removeBtn" v-on:click="groups.splice(index, 1)">x</b-button>
                                </b-col>
                            </b-row>
                            <b-row>
                                <b-col>
                                    <b-form-input v-model="group.name" lazy
                                        style="font-weight: bold; text-align: center; border: 0"></b-form-input>
                                </b-col>
                            </b-row>
                            <PatientCriteria :model="group.criteria" hide-gender="true"></PatientCriteria>
                        </div>
                        <div>
                            <b-button v-on:click="addGroup">Add Group</b-button>
                        </div>
                        <div>
                            <b-button v-if="this.outcomes.length === 1" style="margin-top:15px" v-on:click="discoverGroups">                                <b-icon icon="stars" :style="{ opacity: correlationDiscoveryIconOpacity }"></b-icon>
                                Find High Risk Groups
                            </b-button>
                        </div>
                        <b-row style="margin-top:15px">
                            <b-col>
                                <b-check v-model="includeGroupNoneOfAbove">Include group for all other patients</b-check>
                            </b-col>
                        </b-row>
                    </b-card-text>
                </b-card>
            </div>
            <b-button style="margin-top: 15px; margin-bottom:100px" v-on:click="runReport">Run Report</b-button>
            <!-- <b-button v-on:click="save">Save</b-button> -->
            <!-- <b-button v-on:click="load">Load</b-button> -->
        </b-col>
        <b-col style="margin-top:300px">
<!--            <div hidden>{{conditionsTrigger}}</div>-->
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
import { ClinicalEventCriterionModel } from '@/model/ClinicalEventCriterionModel'
import PatientCriteria from './PatientCriteria.vue'
import { PatientCriteriaModel } from '@/model/PatientCriteriaModel'
import AddCriteriaDropdown from './AddCriteriaDropdown.vue'
import ReportChart from './ReportChart.vue';

export default defineComponent({
    name: 'OutcomeComparison',
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
            groups: [
            ],
            includeGroupNoneOfAbove: false,
            discovering: false,
            outcomes: new Array<ClinicalEventCriterionModel>(),
            cohortSize: "0",
            numberFormat: new Intl.NumberFormat('en-US'),

            // apex
            series: [{data: []}],
            colors: ['#FA8989','#FF924C','#FFCA3A','#C5CA30','#8AC926','#52A675','#1982C4','#4267AC','#6A4C93'],
            correlationDiscoveryIconOpacity: 1,
        }
    },
    mounted() {
        // this.load()
    },
    computed: {
        // Used to monitor changes in selection criteria and trigger API interactions
        patientCriteriaTrigger() {
            const selectionHash: any = this.cohortCriteria.getForAPI()
            // this.save()
            this.updateCohortSize()
            return selectionHash;
        }
    },
    methods: {
        load() {
            axios.get('api/ui-state/groups/dev')
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
                    this.loaded = true
                } else {
                    this.cohortCriteria.eventCriteria.push()
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
            }
            // console.log(model);
            axios.post('api/ui-state/groups/dev', model);
            console.log("saved:", this.cohortCriteria.gender);
        },
        addOutcome(display: string, eclBinding: string) {
            let outcome = new ClinicalEventCriterionModel(display, eclBinding)
            let colorsUsed = new Array<string>()
            this.outcomes.forEach(outcome => {
                if (outcome.color) {
                    colorsUsed.push(outcome.color)
                }
            })
            const colorsLeft = this.colors.filter(c => !colorsUsed.includes(c))
            console.log("colorsLeft", colorsLeft);
            
            if (colorsLeft.length != 0) {
                outcome.color = colorsLeft[0]
            }
            this.outcomes.push(outcome)

        },
        addGroup() {
            this.groups.push({name: "", criteria: new PatientCriteriaModel()})
        },
        discoverGroups() {
            console.log('Discover Correlations...')
            this.discovering = true;
            // Create Correlation Discovery Report request
            const reportRequest = {} as any;
            reportRequest.baseCriteria = this.cohortCriteria.getForAPI()
            let outcome = this.outcomes[0];
            console.log(outcome.conceptECL)
            reportRequest.negativeOutcomeECL = outcome.conceptECL
            this.discoverCorrelations(reportRequest)
        },
        discoverCorrelations(reportRequest: any) {
            // eslint-disable-next-line
            const context = this;
            axios.post('api/correlation-discovery-report', reportRequest)
                .then(response => {
                    context.discovering = false;
                    for (const node of response.data.nodes) {
                        let patientCriteriaModel = new PatientCriteriaModel();
                        let eventCriteria = new ClinicalEventCriterionModel(node.label, "<<" + node.conceptId);
                        eventCriteria.conceptECL = "<<" + node.conceptId + " |" + node.label + "|";
                        eventCriteria.display = node.label;

                        patientCriteriaModel.eventCriteria.push(eventCriteria)
                        context.groups.push({name: node.label+"*", criteria: patientCriteriaModel})
                    }
                })
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
        runReport: function() {
            const reportRequest = this.getReportRequest()
            this.updateOutcomes(reportRequest);
            return reportRequest;
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
            if (this.includeGroupNoneOfAbove) {
                const negativeGroupCriteria = {} as any;
                negativeGroupCriteria.name = "All other patients";
                const exclusionCriteria = [] as Array<any>
                patientGroups.forEach(patientGroup => {
                    exclusionCriteria.push(patientGroup.criteria)
                })
                negativeGroupCriteria.criteria = {
                    exclusionCriteria: exclusionCriteria
                }
                patientGroups.push(negativeGroupCriteria)
            }

            const outcomesRequest = new Array<unknown>();
            const colors = new Array<string>();
            this.outcomes.forEach(outcome => {
                if (outcome.isFilled()) {
                    colors.push(outcome.color)
                    const outcomeCriteria = {} as any;
                    outcomeCriteria.criteria = {
                        eventCriteria: [outcome.getForAPI()]
                    }
                    outcomeCriteria.name = outcome.display
                    outcomesRequest.push(outcomeCriteria)
                }
            })
            report.groups = [patientGroups, outcomesRequest];
            report.colors = colors
            return report;
        }
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
@keyframes pulse {
    0% {
        opacity: 1.0;
    }
    50% {
        opacity: 0.5;
    }

    100% {
        opacity: 1.0;
    }
}
</style>