<template>
    <b-dropdown :text="label" variant="primary" class="m-2">
        <b-dropdown-item v-on:click="$emit('add-criterion', 'Clinical Finding', '<404684003')">Clinical Finding</b-dropdown-item>
        <b-dropdown-item v-on:click="$emit('add-criterion', 'Disorder', '<64572001')">Disorder</b-dropdown-item>
        <b-dropdown-item v-on:click="$emit('add-criterion', 'Procedure', '<71388002')">Procedure</b-dropdown-item>
        <b-dropdown-item v-on:click="$emit('add-criterion', 'Observable', '<363787002')">Observable</b-dropdown-item>
        <b-dropdown-item v-on:click="$emit('add-criterion', 'Pharma / biological product', '<373873005')">Pharma / biological product</b-dropdown-item>
    </b-dropdown>
</template>
<script lang="ts">
import { defineComponent } from 'vue'

export default defineComponent({
    name: "AddCriteriaDropdown",
    props: {
        label: {
            type: String,
            default: "Add Requirement"
        }
    }
})
</script>
