import router, { resetRouter } from '../router'

export default {
  namespaced: true,
  state: {
    staff: JSON.parse(localStorage.getItem('staff')),
    loginType: localStorage.getItem('loginType') || 'staff' // 从本地存储读取登录类型
  },
  mutations: {
    SET_STAFF (state, staff) {
      state.staff = staff
      localStorage.setItem('staff', JSON.stringify(staff)) // 存储用户信息到浏览器
    },
    SET_LOGIN_TYPE (state, loginType) {
      state.loginType = loginType
      localStorage.setItem('loginType', loginType) // 存储登录类型到浏览器
    },
    SET_AVATAR (state, avatar) {
      state.staff.avatar = avatar
      localStorage.setItem('staff', JSON.stringify(state.staff)) // 存储用户信息到浏览器
    },
    CLEAR_LOGIN_TYPE (state) {
      state.loginType = 'staff'
      localStorage.removeItem('loginType')
    }
  },
  actions: {
    // 退出登录
    logout ({ commit }) {
      // 清除本地缓存
      localStorage.clear()
      // 清除vuex中的菜单数据
      commit('menu/CLEAR_MENU', null, { root: true })
      // 清除vuex中的标签数据
      commit('tag/CLEAR_TAG', null, { root: true })
      // 清除登录类型
      commit('CLEAR_LOGIN_TYPE')
      // 清除内存中的 token，避免刷新前继续携带已失效的 token
      commit('token/CLEAR_TOKEN', null, { root: true })
      // 重置路由
      resetRouter()
      router.push({ name: 'login' }).then(() => {
      })
    }
  }
}
