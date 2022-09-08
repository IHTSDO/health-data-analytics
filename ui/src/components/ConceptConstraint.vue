<template>
    <div>
        <b-form-input
                v-model="searchInput"
                debounce="500"
                name="gender-options3"
                v-b-tooltip.right
                :title="selected.codeTerm"
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
// import axios from 'axios'

export default {
    name: 'ConceptConstraint',
    props: {
        constraint: Object
    },
    data() {
        return {
            searchInput: '',
            firstResult: '',
            showResults: true,
            searchResults: [],
            itemJustSelected: false,
            selected: {}
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
            axios.get('health-analytics-api/concepts?prefix=' + input + '&ecl=<<404684003&limit=10')
            .then(response => {
                this.firstResult = response.data[0].display;
                this.searchResults = response.data;
                this.$refs.searchResults.show();
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