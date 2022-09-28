<template>
    <div>
        <apex-chart v-if="!hideChart" ref="outcomeChart" type="bar" height="450" :options="chartOptions" :series="series"></apex-chart>
    </div>
</template>
<script lang="ts">
import { defineComponent, nextTick } from 'vue'
import axios from 'axios'

export default defineComponent({
    name: "ReportChart",
    props: {
        
    },
    data() {
        return {
            myNumbers: new Array<Array<{groupSize: number, subGroupSize: number}>>(),
            series: [],
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
                },
                tooltip: {
                    y: {
                        formatter: function(value, { series, seriesIndex, dataPointIndex, w }) {
                            return value + "%"
                        }
                    }
                }
            },
        }
    },
    methods: {
        async fetchReport(report: {colors: any}) {
            console.log('updating outcome stats', report)

            this.myNumbers.length = 0
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
                    let series = new Array<{name?: string, data: Array<any>, numbers: Array<any>}>();
                    // let reportStats = new Array<Array<{groupSize: number, subGroupSize: number}>>()
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
                                        series.push({data: [], numbers: []});
                                    }
                                    series[i].data.push(0);// bars in with 0 count initially to avoid diagonal animation
                                    i++
                                })
                        }
                    })
                    // this.reportStats = reportStats
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
                                    // console.log("here")
                                    series.push({
                                        name: subGroup.name,
                                        data: [],
                                        numbers: []
                                    });
                                }
                                let percent = (subGroup.patientCount / group.patientCount) * 100
                                percent = Math.round(percent * 100) / 100
                                series[i].data.push(percent)
                                series[i].numbers.push({groupSize: group.patientCount, subGroupSize: subGroup.patientCount})
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
