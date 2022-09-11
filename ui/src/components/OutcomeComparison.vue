<template>
    <b-row>
        <b-col cols="4">
            <div>
                <b-card
                    title="Patient Cohort"
                    tag="article"
                    style="max-width: 20rem;"
                    class="mb-2">
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
                        <div hidden>{{patientCriteriaTrigger}}</div>
                    </b-card-text>
                </b-card>
                <!-- <b-card
                    title="Patient Groups"
                    tag="article"
                    style="max-width: 20rem;"
                    class="mb-2">
                    <b-card-text>
                        <b-form-group v-for="group in group" v-bind:key="group.name">
                            <ConceptConstraint :constraint="outcome" v-on:update:constraint="outcome = $event"/>
                        </b-form-group>
                        <b-button v-on:click="outcomes.push({})">Add</b-button>
                        <div hidden>{{conditionsTrigger}}</div>
                    </b-card-text>
                </b-card> -->
                <b-card
                    title="Compare Outcomes"
                    tag="article"
                    style="max-width: 20rem;"
                    class="mb-2">
                    <b-card-text>
                        <b-form-group v-for="outcome in outcomes" v-bind:key="outcome.ecl">
                            <ConceptConstraint :constraint="outcome" v-on:update:constraint="outcome.selected = $event; updateOutcomes()"/>
                        </b-form-group>
                        <b-button v-on:click="outcomes.push({})">Add</b-button>
                        <!-- <div hidden>{{conditionsTrigger}}</div> -->
                    </b-card-text>
                </b-card>
            </div>
        </b-col>
        <b-col>
            <apex-chart v-bind:class="{ displayNone: hideChart }" ref="outcomeChart" type="bar" height="450" :options="chartOptions" :series="series"></apex-chart>
        </b-col>
    </b-row>
</template>
<script>
import axios from 'axios'
import debounce from 'debounce'
import ConceptConstraint from './ConceptConstraint'

export default {
    name: 'OutcomeComparison',
    components: {
        ConceptConstraint,
    },
    mounted() {
        this.updateCohortSize();
        this.addOutcome();
    },
    data() {
        return {
            gender: '',
            genderOptions: [
            {text: 'All', value: ''},
            {text: 'Female', value: 'FEMALE'},
            {text: 'Male', value: 'MALE'},
            ],
            condition: {initial: "69896004"},
            outcomes: [],
            cohortSize: 0,
            numberFormat: new Intl.NumberFormat('en-US'),

            // apex
            hideChart: true,
            series: [{data: []}],
            chartOptions: {
                chart: {
                    type: 'bar',
                    height: 350
                },
                plotOptions: {
                    bar: {
                        borderRadius: 4,
                        horizontal: true,
                    }
                },
                dataLabels: {
                    enabled: true
                },
                xaxis: {
                    categories: [],
                }
            },
        }
    },
    computed: {
        patientCriteriaTrigger() {
            let selectionHash = this.gender + this.condition;
            this.updateCohortSize()
            return selectionHash;
        },
    },
    methods: {
        addOutcome() {
            this.outcomes.push({
                id: this.random()
            })
        },
        updateCohortSize: debounce(function() {
            console.log('updating cohort size')
            axios.post('health-analytics-api/cohorts/select', this.getPatentCriteria())
                .then(response => {
                    this.cohortSize = this.numberFormat.format(response.data.totalElements);
                })
        }, 100),
        updateOutcomes: debounce(function() {
                console.log('updating outcome stats')
                let report = {};
                report.criteria = this.getPatentCriteria();

                // let patientGroups = [];
                // patientGroups.push({
                //     criteria: {
                //         encounterCriteria: [
                //             { "conceptECL": this.conditionA.ecl }
                //         ]
                //     }
                // })
                // patientGroups.push({
                //     criteria: {
                //         encounterCriteria: [
                //             { "conceptECL": this.conditionB.ecl }
                //         ]
                //     }
                // })
                let outcomesRequest = [];
                this.outcomes.forEach(outcome => {
                    if (outcome.selected) {
                        outcomesRequest.push({
                            "name": outcome.selected.display,
                            criteria: {
                                encounterCriteria: [
                                    {
                                        "conceptECL": outcome.selected.ecl
                                    }
                                ]
                            }
                        })
                    }
                })
                report.groups = [outcomesRequest];
                this.hideChart = false
                axios.post('health-analytics-api/report', report)
                    .then(response => {
                        let data = response.data
                        console.log(data)
                        let labels = ['Cohort Total'];
                        let counts = [0];
                        data.groups.forEach(group => {
                            labels.push(group.name)
                            counts.push(0)
                        })
                        this.$refs.outcomeChart.updateOptions({xaxis: {categories: labels}}, true, true, true);
                        this.$refs.outcomeChart.updateSeries([{data: counts}], false);
                        let context = this
                        setTimeout(function() {
                            let counts = [data.patientCount];
                            data.groups.forEach(group => {
                                counts.push(group.patientCount)
                            })
                            context.$refs.outcomeChart.updateSeries([{data: counts}], true);
                        }, 100)
                    })

        }, 1000),
        getPatentCriteria() {
            let criteria = {};
            if (this.gender != '') {
                criteria.gender = this.gender;
            }
            if (this.condition.ecl) {
                criteria.encounterCriteria = [{ conceptECL: this.condition.ecl }]
            }
            return criteria;
        },
        random: function() {
            return Math.floor(Math.random() * 100000000);
        }
    }
}
</script>
<style scoped>
h3 {
    margin: 40px 0 0;
}
.displayNone {
    display: none;
}
</style>
<style>
legend {
    text-align: left;
    font-weight: bold;
}
</style>