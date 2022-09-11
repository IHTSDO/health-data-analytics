import Vue from 'vue'
import debounce from "lodash.debounce";
import { BootstrapVue, BootstrapVueIcons } from 'bootstrap-vue'
import VueApexCharts from 'vue-apexcharts'
import App from './App.vue'


import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue/dist/bootstrap-vue.css'

Vue.config.productionTip = false
Vue.use(debounce)
Vue.use(BootstrapVue)
Vue.use(BootstrapVueIcons)
Vue.component('apex-chart', VueApexCharts)

new Vue({
  render: h => h(App),
}).$mount('#app')
