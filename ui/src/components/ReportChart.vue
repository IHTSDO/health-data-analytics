<template>
    <div>
        <apex-chart v-bind:class="{ displayNone: hideChart }" ref="outcomeChart" type="bar" height="450" :options="chartOptions" :series="series"></apex-chart>
    </div>
</template>
<script lang="ts">
import { defineComponent } from 'vue'
import axios from 'axios'

export default defineComponent({
    name: "ReportChart",
    props: {
        series: Array
    },
    data() {
        return {
            hideChart: true,
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
    methods: {
        fetchReport(report: {colors: any}) {
            console.log('updating outcome stats', report)

            this.hideChart = false
            axios.post('api/report', report)
                .then(response => {
                    const data = response.data as {
                            groups: Array<{ 
                                name: string
                                patientCount: number
                                groups: Array<{
                                    name: string, patientCount: number
                                }> 
                            }>
                        }
                    console.log(data)
                    const labels = new Array<string>();
                    let series = new Array<{name?: string, data: Array<any>}>();
                    if (!data.groups) {
                        data.groups = [];
                    }
                    data.groups.forEach((group) => {
                        let i = 0
                        labels.push(group.name)
                        // eslint-disable-next-line
                        if (group.groups) {
                            group.groups.forEach(subGroup => {
                                    if (series.length <= i) {
                                        series.push({data: []});
                                    }
                                    series[i].data.push(0);// bars in with 0 count initially to avoid diagonal animation
                                    i++
                                })
                        }
                    })
                    this.$refs.outcomeChart.updateOptions({
                        xaxis: {categories: labels, min: 0, max: 100}, 
                        colors: report.colors
                        // colors: ['#25ACB8', '#F8A73D']
                        // colors: ['#25ACB8', '#F8A73D', '#8072AC']
                    }, true, true, true);
                    this.$refs.outcomeChart.updateSeries(series, false);
                    // eslint-disable-next-line
                    let context = this
                    // Update chart again with correct values
                    setTimeout(function() {
                        series.length = 0
                        
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
                        console.log("series", series);
                        context.$refs.outcomeChart.updateSeries(series, true);
                    }, 200)
                })
        }
    }
})
</script>
<style scoped>
.displayNone {
    display: none;
}
</style>
