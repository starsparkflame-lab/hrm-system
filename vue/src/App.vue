<template>
  <div id="app">
    <router-view />
    <!-- 加载路由视图 -->
  </div>
</template>
<script>
export default {
  name: 'App'
}
</script>

<style>
html,
body {
  margin: 0;
  padding: 0;
  overflow-x: auto;
}

#app {
  color: #2c3e50;
  height: 100%;
  width: 100%;
  position: relative;
}

* {
  box-sizing: border-box;
}
</style>
