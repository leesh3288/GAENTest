import Vue from 'vue'
import App from './App.vue'
import io from 'socket.io-client';
const socket = io('http://192.249.19.252:2880'); 

Vue.prototype.$socket = socket;
Vue.config.productionTip = false

new Vue({
  render: h => h(App)
}).$mount('#app')
