<template>
  <div class="log">
    <h2 id="log-title">Log</h2>
    <ul id="log-wrapper">
      <li class="log-text" v-for="log in logs" v-bind:key="log.id">{{log.msg}}</li>
    </ul>
  </div>
</template>

<script>
import {eventBus} from '../bus'
var logId = 2;

export default {
  name: 'Log',
  data: function() {
    return {
      logs: [{
        msg: "asdfasdf",
        id: 0
      },
      {
        msg: "fdsafdsa",
        id: 1
      }]
    }
  },
  methods: {
    addLog: function(msg) {
      this.logs.unshift({
        msg: msg,
        id: logId
      });
      logId = logId + 1;
    },
    clearLog: function() {
      this.logs = []
    }
  },
  created() {
    this.addLog("asdfasdfasdfsdaf");
    eventBus.$on('log', (data) => {
      this.addLog(data);
      console.log(data); //abc
    });
    eventBus.$on('clearLog', () => {
      this.clearLog();
    })
    this.$socket.on('refuse-console', () => {
      this.addLog('[ERROR] Connection refused. Another console page is active.');
    });
    this.$socket.on('init-console-fail', () => {
      this.addLog('[ERROR] Connection refused. Failed to fetch config.');
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
.log {
  overflow: auto;
  display: grid;
  grid-template: 50px 1fr / 1fr;
}
#log-wrapper {
  align-content: left;
  align-items: left;
  text-align: left;
  overflow: auto;
}
.log-text {
  overflow: auto;
  text-overflow: clip;
  padding-left: 10px;
  padding-right: 10px;
  text-align: left;
  width: 90%;
}
.log {
  height: 100%;
}
.log-grid {
  display: grid;
  grid-template: 4fr 1fr / 1fr;
  height: 100%;
}
</style>

