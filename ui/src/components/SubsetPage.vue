<template>
<div>
    <b-row>
        <h3>Subsets</h3>
    </b-row>
    <b-row>
        <b-col cols="4" style="border-right: 1px solid lightgrey">
            <b-nav vertical pills>
                <b-nav-item v-for="subset in subsets" v-bind:key="subset.id">
                    <router-link 
                        v-bind:class="{ active : selectedSubset.id == subset.id }" 
                        :to="{ name: 'subsets', params: { id: subset.id }}" class="nav-link">{{subset.name}}</router-link>
                </b-nav-item>
            </b-nav>
            <b-button v-on:click="addSubset">Add</b-button>
        </b-col>
        <b-col>
            <div v-if="selectedSubset.id">
                <h6 style="text-align: left">Name: </h6>
                <b-form-input v-model="selectedSubset.name"/>
                <h6 style="text-align: left; margin-top: 15px">ECL Builder: </h6>
                <snomed-ecl-builder
                    apiurl="/api/snowstorm"
                    branch="MAIN"
                    :eclstring="selectedSubset.ecl"
                    v-on:eclOutput="eclChange"
                    />
                <h6 style="text-align: left; margin-top: 15px">ECL Output: </h6>
                <b-textarea disabled v-model="newEcl" rows="5"/>
                <b-row style="margin-top: 15px">
                    <b-col>
                    </b-col>
                    <b-col>
                        <b-button v-on:click="saveSubset">Save</b-button>
                    </b-col>
                    <b-col>
                        <b-button v-on:click="deleteSubset" style="float:right">Delete</b-button>
                    </b-col>
                </b-row>
                <b-row style="margin-top: 15px">
                    <b-col v-if="matchingConcepts.length">
                        <h6 style="text-align: left; margin-top: 15px">{{matchingConceptsCount}} Matching Concepts (first 100 listed): </h6>
                        <b-table striped hover :items="matchingConcepts"></b-table>
                    </b-col>
                    <b-col v-else>
                        <b-button v-on:click="findConcepts">Find matching concepts</b-button>
                    </b-col>
                </b-row>
            </div>
            <div v-else>
                Create or select a subset to edit the ECL
            </div>
        </b-col>
    </b-row>
</div>
</template>
<script lang="ts">
import { defineComponent } from 'vue'
import axios from 'axios';
import { SubsetModel } from '../model/SubsetModel'

export default defineComponent({
    name: "SubsetPage",
    data() {
        return {
            subsets: [] as Array<SubsetModel>,
            selectedSubset: new SubsetModel('', ''),
            matchingConcepts: [],
            matchingConceptsCount: 0,
            newEcl: "",
        }
    },
    created() {
        this.$watch(
            () => this.$route.params,
            (toParams, previousParams) => {
                if (toParams) {
                    this.selectUsingUrl()
                }
            }
        )
    },
    mounted() {
        this.loadSubsets();
        console.log("Route param", this.$route.params.id);
    },
    methods: {
        eclChange(eclChangeEvent: any) {
            this.newEcl = eclChangeEvent.detail[0]
            console.log("eclChange", this.newEcl);
            this.matchingConcepts.length = 0
        },
        findConcepts() {
            this.matchingConcepts.length = 0
            var ecl = this.selectedSubset.ecl
            if (this.newEcl) {
                ecl = this.newEcl
            }
            axios.get('/api/snowstorm/MAIN/concepts?ecl=' + encodeURI(ecl))
            .then(response => {
                response.data.items.forEach(concept => {
                    this.matchingConcepts.push(
                        {
                            id: concept.conceptId,
                            PreferredTerm: concept.pt.term,
                        }
                    )
                })
                this.matchingConceptsCount = response.data.total
            })
        },
        selectUsingUrl() {
            const id = this.$route.params.id
            if (id) {
                this.loadSubset(id)
            } else {
                this.selectedSubset = new SubsetModel('', '')
            }
        },
        loadSubsets() {
            axios.get("/api/subsets")
            .then(response => {
                console.log("Loaded subsets");
                const page = response.data
                const subsetsFromApi = page.content
                this.subsets.length = 0
                subsetsFromApi.forEach(subsetsFromApi => {
                    this.subsets.push(this.createSubsetFromApi(subsetsFromApi))
                });
                this.selectUsingUrl()
            })
        },
        addSubset() {
            const newSubset = new SubsetModel(this.uuidv4(), "new")
            newSubset.ecl = "<< 404684003 |Clinical finding (finding)|"
            axios.put("/api/subsets/" + newSubset.id, newSubset)
            .then(response => {
                this.subsets.push(newSubset)
            })
        },
        loadSubset(id: string) {
            axios.get("/api/subsets/" + id)
            .then(response => {
                console.log("Loaded subset ", id);
                this.selectedSubset = this.createSubsetFromApi(response.data)
                console.log(this.selectedSubset);
            })
        },
        saveSubset() {
            const toSave = this.selectedSubset.clone();
            toSave.ecl = this.newEcl
            axios.put("/api/subsets/" + toSave.id, toSave)
            .then(response => {
                console.log("saved " + response.data.id);
                this.loadSubsets()
                this.loadSubset(toSave.id)
            })
        },
        deleteSubset() {
            axios.delete("/api/subsets/" + this.selectedSubset.id)
            .then(response => {
                console.log("deleted " + response.data.id);
                this.loadSubsets()
                this.$router.push({ name: 'subsetList'})
            })
        },
        createSubsetFromApi(subsetFromApi: any) {
            const subset = new SubsetModel(subsetFromApi.id, subsetFromApi.name)
            subset.description = subsetFromApi.description
            subset.ecl = subsetFromApi.ecl
            return subset;
        },
        uuidv4() {
            return ([1e7]+-1e3+-4e3+-8e3+-1e11).replace(/[018]/g, c =>
                (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
            );
        },
    }
})
</script>
