<template>
  <div id="app" class="container">
    <b-container>
      <b-row>
        <b-col style="padding: 0">
          <b-navbar toggleable="lg" type="dark">
            <b-collapse id="nav-collapse" is-nav>
              <b-navbar-nav>
                <router-link to="/" class="nav-link">Home</router-link>
                <router-link to="/treatment-comparison" class="nav-link">Treatment Comparison</router-link>
                <router-link to="/group-comparison" class="nav-link">Patient Group Comparison</router-link>
                <router-link to="/longitudinal-comparison" class="nav-link">Longitudinal Comparison</router-link>
              </b-navbar-nav>

              <!-- Right aligned nav items -->
              <b-navbar-nav class="ml-auto">
                <b-navbar-brand href="#">SNOMED Analytics Demo (prototype)</b-navbar-brand>
              </b-navbar-nav>
            </b-collapse>
          </b-navbar>
        </b-col>
      </b-row>
      <router-view></router-view>
    </b-container>
  </div>
</template>

<script>

export default {
  name: 'App',
  components: {
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
.navbar {
  background-color: rgba(0,169,224,255);
  margin-bottom: 10px;
}
.navbar .navbar-nav {
  font-weight: 500;
}
.navbar .navbar-nav a.router-link-exact-active {
    color: rgba(255, 255, 255, 0.9);
}
</style>
