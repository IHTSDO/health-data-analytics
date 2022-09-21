import Vue from 'vue'
import VueRouter from 'vue-router'
import debounce from "lodash.debounce";
import { BootstrapVue, BootstrapVueIcons } from 'bootstrap-vue'
import VueApexCharts from 'vue-apexcharts'
import 'snomed-ecl-builder-vue'
import App from './App.vue'
import HomePage from './components/HomePage.vue'
import SubsetPage from './components/SubsetPage.vue'
import TreatmentComparisonPage from './components/TreatmentComparisonPage.vue'
import OutcomeComparison from './components/OutcomeComparison.vue'
import LongitudinalPage from './components/LongitudinalPage.vue'
import 'reflect-metadata';

import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue/dist/bootstrap-vue.css'

// Vue.config.productionTip = false
Vue.use(VueRouter)
Vue.use(debounce)
Vue.use(BootstrapVue)
Vue.use(BootstrapVueIcons)
Vue.component('apex-chart', VueApexCharts)

// Routing
const routes = [
  { path: '/', component: HomePage },
  { name: 'subsetList', path: '/subsets', component: SubsetPage },
  { name: 'subsets', path: '/subsets/:id', component: SubsetPage },
  { path: '/treatment-comparison', component: TreatmentComparisonPage },
  { path: '/group-comparison', component: OutcomeComparison },
  { path: '/longitudinal-comparison', component: LongitudinalPage },
  
]
const router = new VueRouter({
  mode: 'history',
  routes // short for `routes: routes`
})

const vue = new Vue({
  router,
  render: h => h(App),
}).$mount('#app')

