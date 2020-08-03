<template>
   <div id="app">
     <h1>train-demo</h1>
     <section v-if="errored">
       <p>错误</p>
     </section>
     <section v-else>
       <div v-if="loading">Loading...</div>

       <div>
         k8s: {{ info }}
       </div>

     </section>
   </div>
</template>

<script>
import axios from 'axios';
export default {
  name: 'App',
  data () {
   return {
      info: null,
      loading: true,
      errored: false
    }
  },
  mounted () {
    axios
      .get('http://192.168.1.117:3000/api')
      .then(response => {
        this.info = response.data.name
      })
      .catch(error => {
        console.log(error)
        this.errored = true
      })
      .finally(() => this.loading = false)
  }
}
</script>

