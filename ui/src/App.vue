<template>
  <div id="app" class="container">
    <b-container>
      <b-row>
        <h2>SNOMED Analytics</h2>
      </b-row>
      <OutcomeComparison/>
    </b-container>
  </div>
</template>

<script>
import OutcomeComparison from './components/OutcomeComparison'

export default {
  name: 'App',
  components: {
    OutcomeComparison
  }
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
}
</style>
