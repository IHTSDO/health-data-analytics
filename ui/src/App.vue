<template>
  <div id="app" class="container">
    <b-container>
      <b-row>
        <b-col style="padding: 0">
          <b-navbar toggleable="lg" type="dark">
            <b-collapse id="nav-collapse" is-nav>
              <b-navbar-nav>
                <router-link to="/" class="nav-link">Home</router-link>
                <router-link to="/treatment-comparison" class="nav-link">Treatments</router-link>
                <router-link to="/group-comparison" class="nav-link">Patient Groups</router-link>
                <router-link to="/longitudinal-comparison" class="nav-link">Longitudinal</router-link>
                <router-link to="/subsets" class="nav-link nav-link-subpages">Subsets</router-link>
              </b-navbar-nav>

              <!-- Right aligned nav items -->
              <b-navbar-nav class="ml-auto">
                <b-navbar-brand href="#">Snosight (prototype)</b-navbar-brand>
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
  },
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
  background-color: #00A9E0;
  margin-bottom: 10px;
}
.navbar .navbar-nav {
  font-weight: 500;
}
#app .navbar .navbar-nav a.router-link-exact-active, #app .navbar a.nav-link-subpages.router-link-active {
    color: rgba(255, 255, 255, 0.9);
}
#app .btn-primary {
  color: #fff;
  background-color: #00A9E0;
  border-color: #00A9E0;
  /* background-color: #6DBBA1;
  border-color: #6DBBA1; */
}
#app .removeBtn {
  line-height: 10px;
  background-color: white;
  opacity: 75%;
  color: #CE0037;
  border: 0px;
  float: right;
  padding: 0;
}
</style>
