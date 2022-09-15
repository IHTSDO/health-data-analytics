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
                            <ConceptConstraint :constraint="condition" :eclBinding="'*'" v-on:update:constraint="condition = $event"/>
                        </b-form-group>

                        Cohort Size: {{cohortSize}}
                        <div hidden>{{patientCriteriaTrigger}}</div>
                    </b-card-text>
                </b-card>
                <b-card
                    title="Compare Outcomes"
                    tag="article"
                    style="max-width: 20rem;"
                    class="mb-2">
                    <b-card-text>
                        <b-form-group v-for="outcome in outcomes" v-bind:key="outcome.ecl">
                            <ConceptConstraint :constraint="outcome" :eclBinding="'*'" v-on:update:constraint="outcome.selected = $event; updateOutcomes()"/>
                        </b-form-group>
                        <b-button v-on:click="outcomes.push({})">Add Outcome</b-button>
                        <!-- <div hidden>{{conditionsTrigger}}</div> -->
                    </b-card-text>
                </b-card>
                <b-card
                    title="Patient Groups"
                    tag="article"
                    style="max-width: 20rem;"
                    class="mb-2">
                    <b-card-text>
                        <div v-for="group in groups" v-bind:key="group.name" class="patient-group">
                            Group {{group.name}}
                            <b-form-group v-for="event in group.events" v-bind:key="event.ecl">
                                <ConceptConstraint :constraint="event" :eclBinding="'*'" v-on:update:constraint="event.selected = $event"/>
                            </b-form-group>
                            <b-button v-on:click="group.events.push({})">Add Requirement</b-button>
                        </div>
                        <b-button v-on:click="groups.push({events: []})">Add Group</b-button>
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
        // this.addOutcome();

    },
    data() {
        return {
            gender: '',
            genderOptions: [
            {text: 'All', value: ''},
            {text: 'Female', value: 'FEMALE'},
            {text: 'Male', value: 'MALE'},
            ],
            condition: {initial: "1240581000000104"},
            groups: [
            {
                    name: "Everyone",
                    events: []
                },
                {
                    name: "",
                    events: [
                    {initial: "73211009"},
                    {initial: "38341003"},
                    ]
                }
            ],
            outcomes: [
            {initial: "882784691000119100", color: "#99C2A2"},
            {initial: "419099009", color: "#C5EDAC"},
            ],
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
                    title: {
                        text: "Correlation Percent"
                    },
                    categories: [],
                    min: 0,
                    max: 100
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

                let patientGroups = [];
                this.groups.forEach(group => {
                    let groupCriteria = {};
                    patientGroups.push(groupCriteria)
                    groupCriteria.name = group.name;
                    groupCriteria.criteria = {}
                    groupCriteria.criteria.encounterCriteria = [];
                    group.events.forEach(event => {
                        groupCriteria.criteria.encounterCriteria.push({
                            "conceptECL": event.selected.ecl
                        })
                        if (!groupCriteria.name) {
                            groupCriteria.name = event.selected.display
                        }
                    })
                })

                let outcomesRequest = [];
                let colors = [];
                this.outcomes.forEach(outcome => {
                    if (outcome.selected) {
                        colors.push(outcome.color)
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
                report.groups = [patientGroups, outcomesRequest];
                this.hideChart = false
                axios.post('health-analytics-api/report', report)
                    .then(response => {
                        let data = response.data
                        console.log(data)
                        let labels = [];
                        let series = [];
                        if (!data.groups) {
                            data.groups = [];
                        }
                        data.groups.forEach(group => {
                            let i = 0
                            labels.push(group.name)
                            // eslint-disable-next-line
                            group.groups.forEach(subGroup => {
                                    if (series.length <= i) {
                                        series.push({data: []});
                                    }
                                    series[i].data.push(0);// bars in with 0 count initially to avoid diagonal animation
                                    i++
                                })
                        })
                        this.$refs.outcomeChart.updateOptions({xaxis: {categories: labels, min: 0, max: 100}, colors}, true, true, true);
                        this.$refs.outcomeChart.updateSeries(series, false);
                        let context = this
                        // Update chart again with correct values
                        setTimeout(function() {
                            series = [];
                            
                            data.groups.forEach(group => {
                                let i = 0
                                group.groups.forEach(subGroup => {
                                    if (series.length <= i) {
                                        console.log("here")
                                        series.push({
                                            name: subGroup.name,
                                            data: []
                                        });
                                    }
                                    let percent = (subGroup.patientCount / group.patientCount) * 100
                                    percent = Math.round(percent * 100) / 100
                                    series[i].data.push(percent)
                                    i++
                                })
                            })
                            context.$refs.outcomeChart.updateSeries(series, true);
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