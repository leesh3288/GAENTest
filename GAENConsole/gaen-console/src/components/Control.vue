<template>
  <div class="hello">
    <h2>control</h2>
    <label for="testid">Test ID </label>
    <input type="text" id="testid" class="testid form-control" v-model="test.id"/>
    <div class="hello-grid">
      <button id="start-button" class="btn start-button" v-on:click="startExperiment()">Start</button>
      <button id="stop-button" class="btn stop-button" v-on:click="stopExperiment()">Stop</button>
      <button id="clear-button" class="btn clear-button" v-on:click="clearLog()">Clear log</button>
    </div>
  </div>
</template>

<script>
import {eventBus} from '../bus'

// var enabled = false;

export default {
  name: 'HelloWorld',
  data: function () {
  return {
    test: {
      id: ''
    }}
  },
  methods: {
    startExperiment: function () {
      console.log(this.test.id);
      eventBus.$emit('log', this.test.id);
      this.$socket.emit('test');
      // this.$socket.emit('startExperiment',{
      //   testId: document.getElementById("testid").textContent
      // });
    },
    stopExperiment: function () {
      console.log(this.test.id);
      eventBus.$emit('log', this.test.id);
      // this.$socket.emit('startExperiment',{
      //   testId: document.getElementById("testid").textContent
      // });
    },
    clearLog: function() {
      eventBus.$emit('clearLog');
    }
  },
  created() {
    this.$socket.on('init-console', (data) => {
      // enabled = true;
      console.log(data);
    });
    this.$socket.on('refuse-console', (data) => {
      // enabled = false;
      console.log(data);
    });
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
h2 {
  margin: 0;
}
ul {
  list-style-type: none;
  padding: 0;
}
li {
  display: inline-block;
  margin: 0 10px;
}
a {
  color: #42b983;
}
.hello {
  /* background-color: red; */
  padding-left: 20px;
  padding-right: 20px;
}
.hello-grid {
  display: grid;
  grid-template: 1fr 1fr / 1fr 10px 1fr;
}
.btn {
  margin-top: 10px;
}
.start-button {
  grid-column: 1;
}
.stop-button {
  grid-column: 3;
}
.clear-button {
  grid-column: 1/4;
}
</style>
