export default {
  namespaced: true,
  state: {
    token: localStorage.getItem('token')
  },
  mutations: {
    SET_TOKEN (state, token) {
      state.token = token
      localStorage.setItem('token', token)
    },
    // 登出时同步清除内存中的 token，避免刷新前继续携带已失效的 token 发请求
    CLEAR_TOKEN (state) {
      state.token = null
      localStorage.removeItem('token')
    }
  }
}
