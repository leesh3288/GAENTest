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

var enabled = false;

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
      if (!enabled) {
        eventBus.$emit('log', '[ERROR] This console is disabled.');
        return;
      }
      eventBus.$emit('log', 'Button clicked: Start experiment.');
      this.$socket.emit('start',{
        testId: this.test.id
      });
    },
    stopExperiment: function () {
      if (!enabled) {
        eventBus.$emit('log', '[ERROR] This console is disabled.');
        return;
      }
      eventBus.$emit('log', 'Button clicked: Stop experiment.');
      this.$socket.emit('stop');
    },
    clearLog: function() {
      eventBus.$emit('clearLog');
    }
  },
  created() {
    this.$socket.on('init-console', (data) => {
      enabled = true;
      eventBus.$emit('log', data);
    });
    this.$socket.on('refuse-console', () => {
      enabled = false;
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
