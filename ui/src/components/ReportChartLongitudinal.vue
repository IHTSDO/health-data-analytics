<template>
    <div v-if="createChart">
        <apex-chart ref="outcomeChart" type="line" :options="chartOptions" :series="series"></apex-chart>
    </div>
</template>
<script lang="ts">
import { defineComponent } from 'vue'
import axios from 'axios'

export default defineComponent({
    name: "ReportChartLongitudinal",
    data() {
        return {
            createChart: false,
            series: [
                    // {
                    // name: 'Column A',
                    // type: 'line',
                    // data: [21.1, 23, 33.1, 34, 44.1, 44.9, 56.5, 58.5]
                    // },
                ],
            chartOptions: {
                chart: {
                    height: 350,
                    type: "line",
                    stacked: false
                },
                dataLabels: {
                    enabled: false
                },
                colors: ['#99C2A2', '#C5EDAC', '#66C7F4'],
                markers: {
                    size: 4,
                },
                stroke: {
                    width: [4, 4, 4, 4, 4, 4, 4, 4, 4]
                },
                plotOptions: {
                    bar: {
                        columnWidth: "20%"
                    }
                },
                xaxis: {
                    categories: []
                },
                yaxis: [
                    {
                        seriesName: 'Column A',
                        axisTicks: {
                            show: true
                        },
                        axisBorder: {
                            show: true,
                        },
                        title: {
                            text: "Patients"
                        }
                    },
                ],
                tooltip: {
                    shared: false,
                    intersect: true,
                    x: {
                        show: false
                    }
                },
                legend: {
                    horizontalAlign: "left",
                    offsetX: 40
                }
            },
        }
    },
    setup() {
        // this.$refs.outcomeChart.updateOptions({
        //     xaxis: {categories: ["A"]}, 
        //     // colors: report.colors
        //     colors: ['#25ACB8']
        //     // colors: ['#25ACB8', '#F8A73D', '#8072AC']
        // }, true, true, true);

    },
    methods: {
        async fetchReports(reports: [{colors: any}]) {
            console.log("fetchReports");

            this.chartOptions.xaxis.categories.length = 0
            for (const i in reports) {
                this.chartOptions.xaxis.categories.push(reports[i].name)
            }
            // console.log("this.chartOptions", this.chartOptions);

            this.createChart = true

            // if (100 == 100) {
                // return
            // }
            let initial = true
            for (let index = 0; index < reports.length; index++) {
                await this.fetchReport(reports[index], initial)
                initial = false
            }

        },
        async fetchReport(report: {colors: any}, initial: boolean) {
            console.log('updating outcome stats', report)

            // this.hideChart = false
            await axios.post('api/report', report)
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

                    let series = this.series
                    if (initial) {
                        console.log("initial graph");
                        
                        series.length = 0
                        let i = 0
                        data.groups.forEach(group => {
                            group.groups.forEach(subgroup => {
                                series.push({
                                    name: subgroup.name,
                                    data: []
                                })
                            })
                        })
                    }

                    if (!data.groups) {
                        data.groups = [];
                    }

                    // eslint-disable-next-line
                    let context = this
                    // Update chart again with correct values
                    setTimeout(function() {
                        // series.length = 0
                        
                        data.groups.forEach(group => {
                            let i = 0
                            group.groups.forEach(subGroup => {
                                // if (series.length <= i) {
                                //     console.log("here")
                                //     series[i].data.push({
                                //         name: subGroup.name,
                                //         type: "line",
                                //         data: []
                                //     });
                                // }
                                // let percent = (subGroup.patientCount / group.patientCount) * 100
                                // percent = Math.round(percent * 100) / 100
                                series[i].data.push(subGroup.patientCount)
                                context.$refs.outcomeChart.updateSeries(series, true);

                                i++
                            })
                        })
                        // console.log("series", series);
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
