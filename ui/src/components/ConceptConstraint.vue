<template>
    <div>
        {{display}}
        <b-form-input
                v-model="searchInput"
                debounce="500"
                name="gender-options3"
                v-b-tooltip.right
                :title="selected.codeTerm"
                :style="style"
            ></b-form-input>
        <div class="typeahead">
            <b-dropdown id="dropdown-1" text="Dropdown Button" ref="searchResults"
                >
                <b-dropdown-item v-for="item in searchResults" :key="item.code"
                    v-on:click="selectResult(item)">
                    {{item.display}}
                </b-dropdown-item>
            </b-dropdown>
        </div>
    </div>
</template>
<script>
import axios from 'axios';

export default {
    name: 'ConceptConstraint',
    props: {
        display: String,
        eclBinding: String,
        constraint: Object,
    },
    data() {
        return {
            searchInput: '',
            showResults: true,
            searchResults: [],
            itemJustSelected: false,
            selected: {}
        }
    },
    computed: {
        style() {
            if (this.constraint.color) {
                return "background-color: " + this.constraint.color
            }
            return ""
        }
    },
    mounted() {
        if (this.constraint.initial) {
            this.FHIRSearch(this.constraint.initial);
        }
        if (!this.eclBinding) {
            console.warn('No ECL binding defined.');
        }
    },
    watch: {
        searchInput: function(input) {
            if (this.itemJustSelected) {
                this.itemJustSelected = false;
            } else if (input.length > 2) {
                this.FHIRSearch(input);
            }
        }
    },
    methods: {
        FHIRSearch(input) {
            console.log('FHIRSearch');
            axios.get('health-analytics-api/concepts?prefix=' + input + '&ecl=' + this.eclBinding + '&limit=10')
            .then(response => {
                if (response.data.length == 1) {
                    this.selectResult(response.data[0]);
                } else {
                    this.searchResults = response.data;
                    this.$refs.searchResults.show();
                }
            })
        },
        selectResult(item) {
            this.itemJustSelected = true;
            item.codeTerm = item.code + ' |' + item.display + '|'
            item.ecl = '<< ' + item.codeTerm
            this.selected = item;
            this.searchInput = item.display
            this.$emit('update:constraint', item)
        }
    }
}
</script>
<style>
    .typeahead {
        text-align: left;
        margin-bottom: -26px;
    }
    .typeahead .dropdown-toggle {
        visibility: hidden;
    }
    .typeahead .dropdown {
        margin-top: -36px;
    }
    .typeahead .dropdown-toggle {
        margin-top: -38px;
    }
    .typeahead .dropdown-menu {
        border-top-left-radius: 0;
        border-top-right-radius: 0;
    }
</style>