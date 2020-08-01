<template>
  <div id="app">
    <link rel="stylesheet" href="//maxcdn.bootstrapcdn.com/bootstrap/3.3.2/css/bootstrap.min.css">
    <h1 id="header">GAEN Test App Console</h1>
    <div class="grid body">
      <Status class="status-component"/>
      <Control class="control-component"/>
      <Log class="log-component"/>
    </div>
    <HelloWorld msg="Welcome to Your Vue.js App"/>
  </div>
</template>

<script>
// import HelloWorld from './components/HelloWorld.vue'
import Log from './components/Log'
import Status from './components/Status'
import Control from './components/Control'

export default {
  name: 'App',
  components: {
    Log,
    Status,
    Control
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
  /* background-color: aqua; */
  margin-top: 10px;
}

#header {
  font-size: 40px;
}

.h1 {
  margin-bottom: 40px;
}

.grid {
  display: grid;
  grid-template: 1fr 2fr / 1fr 1fr;
}

.body {
  height: 85vh;
}

.log-component {
  grid-row: 1/3;
  grid-column: 2;
  /* overflow: auto; */
}
.control-component {
  grid-row: 1;
  grid-column: 1;
}
.status-component {
  grid-row: 2;
  grid-column: 1;
}
</style>
