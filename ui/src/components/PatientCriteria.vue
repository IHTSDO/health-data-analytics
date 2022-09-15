<template>
    <div>
        <b-form-group label="Gender">
            <b-form-radio-group
                id="gender-group"
                v-model="gender"
                :options="genderOptions"
                name="gender-options"
            ></b-form-radio-group>
        </b-form-group>
        <b-form-group v-for="eventCriterion in value.eventCriteria" v-bind:key="eventCriterion.ecl" >
            <ConceptConstraint :constraint="eventCriterion" :display="eventCriterion.display" :eclBinding="eventCriterion.eclBinding" v-on:update:constraint="eventCriterion.selected = $event"/>
        </b-form-group>
        <b-dropdown id="dropdown-dropup" text="Add Requirement" variant="primary" class="m-2">
            <b-dropdown-item v-on:click="addEventCriterion('Clinical Finding', '<404684003')">Clinical Finding</b-dropdown-item>
            <b-dropdown-item v-on:click="addEventCriterion('Disorder', '<64572001')">Disorder</b-dropdown-item>
            <b-dropdown-item v-on:click="addEventCriterion('Procedure', '<71388002')">Procedure</b-dropdown-item>
            <b-dropdown-item v-on:click="addEventCriterion('Observable', '<363787002')">Observable</b-dropdown-item>
            <b-dropdown-item v-on:click="addEventCriterion('Pharma / biological product', '<373873005')">Pharma / biological product</b-dropdown-item>
        </b-dropdown>
    </div>
</template>
<script>
import ConceptConstraint from './ConceptConstraint'
export default {
    name: 'PatientCriteria',
    components: {
        ConceptConstraint
    },
    props: {
        value: Object
    },
    created() {
        if (typeof this.value.eventCriteria === 'undefined') {
            this.$set(this.value, 'eventCriteria', []);
        }
    },
    data() {
        return {
            gender: '',
            genderOptions: [
                {text: 'All', value: ''},
                {text: 'Female', value: 'FEMALE'},
                {text: 'Male', value: 'MALE'},
            ],
        }
    },
    methods: {
        addEventCriterion(display, eclBinding) {
            // Use update method for object from parent component
            this.$set(this.value.eventCriteria, this.value.eventCriteria.length, {display, eclBinding})
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

</style>
