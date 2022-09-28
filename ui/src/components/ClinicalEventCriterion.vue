<template>
    <div>
        <div style="font-size: 0.85em; text-align: left; font-style: italic;">{{model?.title}}</div>
        <b-form-input
                v-model="searchInput"
                debounce="500"
                name="gender-options3"
                :style="style"
                autocomplete="off"

                v-b-tooltip.right
                :title="model?.conceptECL + '' + model?.historyECL"
            ></b-form-input>
        <b-dropdown class="input-menu">
            <b-dropdown-item v-on:click="$emit('remove')">Remove</b-dropdown-item>
        </b-dropdown>
        <div class="typeahead">
            <b-dropdown text="Dropdown Button" ref="searchResults">
                <b-dropdown-item v-for="item in searchResults" :key="item.code"
                    v-on:click="selectResult(item)">
                    {{item.display}}
                </b-dropdown-item>
            </b-dropdown>
        </div>
    </div>
</template>
<script lang="ts">
import { defineComponent } from 'vue'
import { ClinicalEventCriterionModel } from '@/model/ClinicalEventCriterionModel';
import axios from 'axios'

export default defineComponent({
    name: 'ClinicalEventCriterion',
    props: {
        model: ClinicalEventCriterionModel
    },
    data() {
        return {
            searchInput: '',
            showResults: true,
            searchResults: [] as Array<DropdownItem>,
            itemJustSelected: false,
        }
    },
    updated() {
        if (this.model?.conceptECL) {
            this.itemJustSelected = true;
            this.searchInput = this.model?.display
        }
    },
    computed: {
        style(): string {
            if (this.model && this.model.color) {
                return "background-color: " + this.model.color + "; color: white;";
            }
            return "";
        }
    },
    mounted() {
        if (this.model && this.model.initial) {
            this.FHIRSearch(this.model.initial);
        }
        if (!this.model?.eclBinding) {
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
        async FHIRSearch(input: string) {
            const matchingSubsets = await (await axios.get('api/subsets?prefix=' + input)).data.content
            
            axios.get('api/concepts?prefix=' + input + '&ecl=' + this.model?.eclBinding + '&limit=10')
            .then(response => {
                if (response.data.length == 1 && matchingSubsets.length == 0) {
                    this.selectResult(response.data[0]);
                } else {
                    this.searchResults = [];
                    matchingSubsets.forEach((element: { id: string; name: string; ecl: string; }) => {
                        this.searchResults.push(new DropdownItem(element.id, element.name + " (subset)", element.ecl))
                    });
                    response.data.forEach((element: { code: string; display: string; }) => {
                        this.searchResults.push(new DropdownItem(element.code, element.display))
                    });
                    if (this.$refs.searchResults) {
                        // eslint-disable-next-line
                        (this.$refs.searchResults as any).show();
                    }
                }
            });
        },
        selectResult(item: {code: string, display: string, ecl?: string}) {
            let selected = new DropdownItem(item.code, item.display, item.ecl)
            if (this.model) {
                this.$set(this.model, 'display', selected.display)
                this.$set(this.model, 'conceptECL', selected.getEcl())
            }
            this.showResults = false;
        }
    }
});

export class DropdownItem {
    code: string
    display: string
    ecl?: string
    subset: boolean

    constructor(code: string, display: string, ecl?: string) {
        this.code = code;
        this.display = display;
        this.ecl = ecl;
        if (ecl) {
            this.subset = true
        } else {
            this.subset = false
        }
    }
    
    public getCodeTerm() : string {
        if (this.subset) {
            return "Subset: " + this.display
        } else {
            return this.code + ' |' + this.display + '|';
        }
    }

    getEcl() {
        if (this.subset) {
            return this.ecl
        } else {
            return "<< " + this.getCodeTerm();
        }
    }

}

</script>
<style scoped>
.input-menu {
    float: right;
    margin-top: -38px;
}
</style>
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