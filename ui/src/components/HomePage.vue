<template>
    <b-row>
        <b-card
            title="Treatment Comparison"
            tag="article"
            style="max-width: 20rem;"
            class="mb-2">
            <b-card-text>
                Compare treatment options<br/> by measuring the correlation between treatment options and outcomes
            </b-card-text>
            <b-button to="/treatment-comparison" variant="primary">Start</b-button>
        </b-card>
        <b-card
            title="Patient Group Comparison"
            tag="article"
            style="max-width: 20rem;"
            class="mb-2">
            <b-card-text>
                Find vulnerable patient groups<br/> by measuring the correlation of a clinical event between different patient groups
            </b-card-text>
            <b-button to="/group-comparison" variant="primary">Start</b-button>
        </b-card>
        <b-card
            title="Longitudinal Comparison"
            tag="article"
            style="max-width: 20rem;"
            class="mb-2">
            <b-card-text>
                Track trends in clinical events or outcomes over time
            </b-card-text>
            <b-button to="/longitudinal-comparison" variant="primary">Start</b-button>
        </b-card>
    </b-row>
</template>
<style scoped>
    .card {
        margin: 10px;
    }
</style>
