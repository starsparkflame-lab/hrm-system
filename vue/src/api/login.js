import request from '../utils/request'

// 登录
export const login = (data) => {
  return request({
    url: '/login',
    method: 'post',
    data
  })
}

// 获取验证码
export const getCaptcha = () => {
  return request({
    url: '/captcha',
    method: 'get'
  })
}

// 退出登录，通知服务端撤销 token
export const logout = () => {
  return request({
    url: '/logout',
    method: 'post'
  })
}

// 注册
export const register = (data) => {
  return request({
    url: '/register',
    method: 'post',
    data
  })
}

// 发送找回密码验证码
export const sendResetCode = (data) => {
  return request({
    url: '/forgot-password/send-code',
    method: 'post',
    data
  })
}

// 重置密码
export const resetPassword = (data) => {
  return request({
    url: '/forgot-password/reset',
    method: 'post',
    data
  })
}
