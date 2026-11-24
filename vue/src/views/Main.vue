<template>
  <el-container style="height: 100%">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="main-aside">
      <common-aside style="width: 100%" :is-collapse="isCollapse"/>
      <!-- 引入侧边栏 -->
    </el-aside>
    <el-container>
      <el-header>
        <common-header :is-collapse="isCollapse" @toggle-sidebar="toggleSidebar"/>
        <!-- 引入顶栏 -->
      </el-header>
      <!-- 引入标签栏 -->
      <common-tag/>
      <el-main>
        <router-view/>
        <!-- 路由视图 -->
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
// @ is an alias to /src
import CommonAside from '../components/CommonAside'
import CommonHeader from '../components/CommonHeader'
import CommonTag from '../components/CommonTag'

export default {
  name: 'Main',
  components: {
    CommonAside,
    CommonHeader,
    CommonTag
  },
  data () {
    return {
      isCollapse: false
    }
  },
  methods: {
    toggleSidebar () {
      this.isCollapse = !this.isCollapse
    }
  }
}
</script>

<style lang="less">
.el-header {
  background-color: #eaeeff;
}

.el-main {
  padding-top: 0;
}

/* 侧边栏过渡动画 */
.main-aside {
  transition: width 0.3s ease;
  position: relative;
  z-index: 100;
  box-shadow: 2px 0 6px rgb(0 21 41 / 35%);
}
</style>
