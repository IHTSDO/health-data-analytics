<template>
    <div>
    <b-row>
        <p style="margin-top:15px; margin-left: 10px">A SNOMED CT enabled clinical data analytics demonstrator using FHIR terminology services.</p>
    </b-row>
    <b-row>
        <b-card
            title="Treatment Comparison"
            tag="article"
            style="max-width: 20rem;"
            class="mb-2">
            <b-card-text>
                Compare treatment options<br/> by measuring the correlation between treatments and outcomes
            </b-card-text>
            <b-button to="/treatment-comparison" variant="primary">Start</b-button>
        </b-card>
        <b-card
            title="Patient Group Comparison"
            tag="article"
            style="max-width: 20rem;"
            class="mb-2">
            <b-card-text>
                Identify vulnerable patient groups<br/> by measuring the correlation between clinical events and patient groups
            </b-card-text>
            <b-button to="/group-comparison" variant="primary">Start</b-button>
        </b-card>
        <b-card
            title="Longitudinal Comparison"
            tag="article"
            style="max-width: 20rem;"
            class="mb-2">
            <b-card-text>
                Track trends over time
            </b-card-text>
            <b-button to="/longitudinal-comparison" variant="primary">Start</b-button>
        </b-card>
    </b-row>
</div>
</template>
<style scoped>
    .card {
        margin: 10px;
    }

</style>
