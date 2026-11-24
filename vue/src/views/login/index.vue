<template>
  <div class="login-container" :style="bgStyle">
    <div class="login-wrapper">
      <!-- 左侧装饰区域 -->
      <div class="login-left">
        <div class="logo-area">
          <div class="logo-circle">
            <i class="el-icon-office-building logo-icon"></i>
          </div>
          <div class="system-title">
            人力资源管理系统
          </div>
          <div class="system-subtitle">
            Human Resource Management System
          </div>
        </div>
        <div class="decorative-text">
          <p class="feature-text">• 高效的人员管理</p>
          <p class="feature-text">• 精准的考勤统计</p>
          <p class="feature-text">• 智能的薪酬计算</p>
          <p class="feature-text">• 完善的绩效评估</p>
        </div>
      </div>
      <!-- 右侧登录表单 -->
      <div class="login-right">
        <div class="login-form-container">
          <div class="welcome-text">
            {{ isRegister ? '欢迎注册' : '欢迎登录' }}
          </div>
          <div class="form-subtitle">
            {{ isRegister ? '请填写您的注册信息' : '请选择登录方式并输入您的账号信息' }}
          </div>
          <!-- 登录类型选择 -->
          <div class="login-type-selector" v-if="!isRegister">
            <div
              class="type-item"
              :class="{ active: loginType === 'admin' }"
              @click="loginType = 'admin'"
            >
              <i class="el-icon-s-custom type-icon"></i>
              <span>管理员登录</span>
            </div>
            <div
              class="type-item"
              :class="{ active: loginType === 'staff' }"
              @click="loginType = 'staff'"
            >
              <i class="el-icon-user-solid type-icon"></i>
              <span>员工登录</span>
            </div>
          </div>
          <!-- 登录/注册切换 -->
          <div class="mode-switch">
            <span @click="isRegister = false" :class="{ active: !isRegister }">登录</span>
            <span class="divider">|</span>
            <span @click="isRegister = true" :class="{ active: isRegister }">注册</span>
          </div>
          <el-form
            :rules="rules"
            :model="staff"
            ref="loginForm"
            class="login-form"
            :class="{ 'register-mode': isRegister }"
          >
            <el-form-item prop="code" class="form-item">
              <div class="input-label">{{ isRegister ? '工号' : '账号' }}</div>
              <el-input
                size="medium"
                :placeholder="isRegister ? '请输入工号' : (loginType === 'admin' ? '请输入管理员账号' : '请输入员工账号')"
                prefix-icon="el-icon-user"
                v-model.trim="staff.code"
                class="custom-input"
              ></el-input>
            </el-form-item>
            <el-form-item prop="name" class="form-item" v-if="isRegister">
              <div class="input-label">姓名</div>
              <el-input
                size="medium"
                placeholder="请输入姓名"
                prefix-icon="el-icon-user"
                v-model.trim="staff.name"
                class="custom-input"
              ></el-input>
            </el-form-item>
            <el-form-item prop="phone" class="form-item" v-if="isRegister">
              <div class="input-label">手机号</div>
              <el-input
                size="medium"
                placeholder="请输入手机号"
                prefix-icon="el-icon-phone"
                v-model.trim="staff.phone"
                class="custom-input"
              ></el-input>
            </el-form-item>
            <el-form-item prop="gender" class="form-item" v-if="isRegister">
              <div class="input-label">性别</div>
              <el-radio-group v-model="staff.gender">
                <el-radio :label="0">男</el-radio>
                <el-radio :label="1">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item prop="password" class="form-item password-item">
              <div class="input-label">{{ isRegister ? '密码' : '密码' }}</div>
              <div class="password-input-wrapper">
                <el-input
                  size="medium"
                  :placeholder="isRegister ? '请设置登录密码' : '请输入登录密码'"
                  prefix-icon="el-icon-lock"
                  show-password
                  v-model.trim="staff.password"
                  class="custom-input"
                  @keyup.enter="handleLogin"
                ></el-input>
                <div class="forgot-password" v-if="!isRegister" @click="handleForgotPassword">忘记密码?</div>
              </div>
            </el-form-item>
            <el-form-item prop="captcha" class="form-item">
              <div class="input-label">验证码</div>
              <div class="captcha-container">
                <el-input
                  size="medium"
                  placeholder="请输入验证码"
                  prefix-icon="el-icon-key"
                  v-model.trim="staff.captcha"
                  class="captcha-input"
                  @keyup.enter="handleLogin"
                ></el-input>
                <div class="captcha-image" @click="refreshCaptcha">
                  <img :src="captchaImage" alt="验证码" />
                </div>
              </div>
            </el-form-item>
            <el-form-item class="form-item">
              <el-button
                :type="isRegister ? 'success' : (loginType === 'admin' ? 'danger' : 'primary')"
                size="medium"
                class="login-button"
                @click="isRegister ? handleRegister() : handleLogin()"
                :loading="loading"
              >
                {{ isRegister ? '立即注册' : (loginType === 'admin' ? '管理员登录' : '员工登录') }}
              </el-button>
            </el-form-item>
          </el-form>
          <div class="footer-tips">
            <div class="tip-text" v-if="!isRegister">
              <i class="el-icon-info"></i>
              初始密码请联系管理员获取
            </div>
            <div class="tip-text" v-else>
              <i class="el-icon-info"></i>
              注册后请联系管理员审核
            </div>
          </div>
        </div>
      </div>
    </div>
    <!-- 找回密码对话框 -->
    <el-dialog
      title="找回密码"
      :visible.sync="resetDialogVisible"
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form :model="resetForm" :rules="resetRules" ref="resetForm" label-width="80px">
        <el-form-item label="员工编号" prop="code">
          <el-input
            v-model="resetForm.code"
            placeholder="请输入员工编号"
            prefix-icon="el-icon-user"
          ></el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="resetForm.phone"
            placeholder="请输入注册时的手机号"
            prefix-icon="el-icon-phone"
          ></el-input>
        </el-form-item>
        <el-form-item label="验证码" prop="captchaCode">
          <div class="reset-captcha-container">
            <el-input
              v-model="resetForm.captchaCode"
              placeholder="请输入验证码"
              prefix-icon="el-icon-key"
              style="flex: 1;"
            ></el-input>
            <el-button
              type="primary"
              size="small"
              @click="handleSendResetCode"
              :disabled="countdown > 0"
              style="margin-left: 10px;"
            >
              {{ countdown > 0 ? `${countdown}s后重试` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="resetForm.newPassword"
            type="password"
            placeholder="请输入新密码(6-20位)"
            prefix-icon="el-icon-lock"
            show-password
          ></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="resetForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            prefix-icon="el-icon-lock"
            show-password
          ></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="resetDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleResetPassword" :loading="resetLoading">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>
<script>
import { login, getCaptcha, register, sendResetCode, resetPassword } from '../../api/login'
export default {
  name: 'Login',
  data () {
    return {
      staff: {
        captcha: '',
        name: '',
        phone: '',
        gender: 0// 默认男性
      },
      loginType: 'staff', // 默认员工登录
      loading: false,
      captchaImage: '',
      captchaKey: '', // 验证码唯一标识，登录/注册时需回传给后端
      isRegister: false, // 是否为注册模式
      // 找回密码相关
      resetDialogVisible: false,
      resetLoading: false,
      countdown: 0,
      countdownTimer: null,
      resetForm: {
        code: '',
        phone: '',
        captchaCode: '',
        newPassword: '',
        confirmPassword: ''
      },
      resetRules: {
        code: [
          { required: true, message: '请输入员工编号', trigger: 'blur' },
          { min: 3, max: 20, message: '长度在3到20个字符', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
        ],
        captchaCode: [
          { required: true, message: '请输入验证码', trigger: 'blur' },
          { min: 6, max: 6, message: '请输入6位验证码', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度在6到20个字符', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入新密码', trigger: 'blur' },
          { validator: this.validateConfirmPassword, trigger: 'blur' }
        ]
      },
      // 效验规则
      rules: {
        code: [
          { required: true, message: '请输入账号', trigger: 'blur' },
          { min: 3, max: 20, message: '长度在3到20个字符', trigger: 'blur' }
        ],
        name: [
          { required: true, message: '请输入姓名', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 3, max: 20, message: '长度在3到20个字符', trigger: 'blur' }
        ],
        captcha: [
          { required: true, message: '请输入验证码', trigger: 'blur' },
          { min: 4, max: 4, message: '请输入4位验证码', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    bgStyle () {
      return {
        backgroundImage: 'url(' + require('@/assets/bg3.png') + ')'
      }
    }
  },
  created () {
    this.refreshCaptcha()
  },

  methods: {
    // 刷新验证码
    refreshCaptcha () {
      getCaptcha().then(response => {
        if (response.code === 200) {
          // 后端返回 { captchaKey, image }，验证码存在服务端缓存中，5分钟有效
          this.captchaImage = response.data.image
          this.captchaKey = response.data.captchaKey
        } else {
          console.error('验证码返回错误:', response.message)
        }
      }).catch(err => {
        console.error('获取验证码失败:', err)
      })
    },
    // 注册
    handleRegister () {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          register({ ...this.staff, captchaKey: this.captchaKey }).then(
            response => {
              this.loading = false
              if (response.code === 200) {
                this.$message.success(response.message || '注册成功！请登录')
                // 注册成功后切换到登录模式
                this.isRegister = false
                // 清空表单
                this.staff.code = ''
                this.staff.name = ''
                this.staff.phone = ''
                this.staff.gender = 0
                this.staff.password = ''
                this.staff.captcha = ''
                this.refreshCaptcha()
              } else {
                this.$message.error(response.message)
                // 注册失败后刷新验证码
                this.staff.captcha = ''
                this.refreshCaptcha()
              }
            }
          ).catch(() => {
            this.loading = false
            this.$message.error('注册失败，请稍后重试')
            // 注册失败后刷新验证码
            this.staff.captcha = ''
            this.refreshCaptcha()
          })
        }
      })
    },
    // 忘记密码
    handleForgotPassword () {
      this.resetDialogVisible = true
      this.resetForm = {
        code: '',
        phone: '',
        captchaCode: '',
        newPassword: '',
        confirmPassword: ''
      }
      this.countdown = 0
      if (this.countdownTimer) {
        clearInterval(this.countdownTimer)
      }
    },
    // 发送找回密码验证码
    handleSendResetCode () {
      this.$refs.resetForm.validateField('phone', (errorMessage) => {
        if (errorMessage) {
          return
        }
        sendResetCode({ phone: this.resetForm.phone }).then(response => {
          if (response.code === 200) {
            this.$message.success(response.message || '验证码已发送！')
            // 开始倒计时
            this.countdown = 60
            this.countdownTimer = setInterval(() => {
              this.countdown--
              if (this.countdown <= 0) {
                clearInterval(this.countdownTimer)
              }
            }, 1000)
          } else {
            this.$message.error(response.message)
          }
        }).catch(() => {
          this.$message.error('发送验证码失败，请稍后重试')
        })
      })
    },
    // 验证确认密码
    validateConfirmPassword (rule, value, callback) {
      if (value === '') {
        callback(new Error('请再次输入新密码'))
      } else if (value !== this.resetForm.newPassword) {
        callback(new Error('两次输入密码不一致！'))
      } else {
        callback()
      }
    },
    // 重置密码
    handleResetPassword () {
      this.$refs.resetForm.validate(valid => {
        if (valid) {
          this.resetLoading = true
          resetPassword({
            code: this.resetForm.code,
            phone: this.resetForm.phone,
            captchaCode: this.resetForm.captchaCode,
            newPassword: this.resetForm.newPassword
          }).then(response => {
            this.resetLoading = false
            if (response.code === 200) {
              this.$message.success(response.message || '密码重置成功！')
              this.resetDialogVisible = false
              // 清空表单
              this.resetForm = {
                code: '',
                phone: '',
                captchaCode: '',
                newPassword: '',
                confirmPassword: ''
              }
              if (this.countdownTimer) {
                clearInterval(this.countdownTimer)
              }
              this.countdown = 0
            } else {
              this.$message.error(response.message)
            }
          }).catch(() => {
            this.resetLoading = false
            this.$message.error('密码重置失败，请稍后重试')
          })
        }
      })
    },
    handleLogin () {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          // 添加登录类型与验证码标识
          const loginData = {
            ...this.staff,
            loginType: this.loginType,
            captchaKey: this.captchaKey
          }
          login(loginData).then(
            response => {
              this.loading = false
              if (response.code === 200) {
                // 保存员工信息、登录类型和token
                // 将loginType合并到staff对象中，方便后续使用
                const staffWithLoginType = {
                  ...response.data.staff,
                  loginType: response.data.loginType
                }
                this.$store.commit('staff/SET_STAFF', staffWithLoginType)
                this.$store.commit('staff/SET_LOGIN_TYPE', response.data.loginType)
                this.$store.commit('token/SET_TOKEN', response.token)
                const typeText = this.loginType === 'admin' ? '管理员' : '员工'
                this.$message.success(`${typeText}登录成功！`)
                this.$router.push({
                  path: '/home'
                })
              } else {
                this.$message.error(response.message)
                // 登录失败后刷新验证码
                this.staff.captcha = ''
                this.refreshCaptcha()
              }
            }
          ).catch(() => {
            this.loading = false
            this.$message.error('登录失败，请稍后重试')
            // 登录失败后刷新验证码
            this.staff.captcha = ''
            this.refreshCaptcha()
          })
        }
      })
    }
  }
}
</script>
<style scoped>
.login-container {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-size: cover;
  background-position: center center;
  background-repeat: no-repeat;
}
.login-wrapper {
  width: 900px;
  height: 680px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
  display: flex;
  overflow: hidden;
  backdrop-filter: blur(10px);
}

/* 左侧区域样式 */
.login-left {
  width: 50%;
  box-sizing: border-box;
  background: linear-gradient(135deg, #b46231 0%, #4fc720 100%);
  padding: 50px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: white;
  position: relative;
  overflow: hidden;
}
.login-left::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 100%;
  height: 200%;
  background: rgba(255, 255, 255, 0.1);
  transform: rotate(30deg);
}
.logo-area {
  margin-bottom: 60px;
  position: relative;
  z-index: 1;
}
.logo-circle {
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 25px;
  backdrop-filter: blur(5px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}
.logo-icon {
  font-size: 40px;
  color: white;
}
.system-title {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 10px;
  letter-spacing: 1px;
}
.system-subtitle {
  font-size: 14px;
  opacity: 0.85;
  letter-spacing: 1.5px;
  text-transform: uppercase;
}
.decorative-text {
  position: relative;
  z-index: 1;
}
.feature-text {
  font-size: 16px;
  margin: 12px 0;
  display: flex;
  align-items: center;
  opacity: 0.9;
  transition: all 0.3s ease;
}
.feature-text:hover {
  opacity: 1;
  transform: translateX(5px);
}
/* 右侧区域样式 */
.login-right {
  width: 50%;
  box-sizing: border-box;
  padding: 30px 50px 30px 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.login-form-container {
  max-width: 300px;
  margin: 0 auto;
  width: 100%;
  padding-top: 20px;
}
.welcome-text {
  font-size: 26px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 6px;
  text-align: center;
}
.form-subtitle {
  font-size: 12px;
  color: #7f8c8d;
  margin-bottom: 12px;
  text-align: center;
}
/* 登录类型选择器样式 */
.login-type-selector {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.type-item {
  flex: 1;
  padding: 12px 8px;
  border: 2px solid #dcdfe6;
  border-radius: 8px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #fff;
}
.type-item:hover {
  border-color: #3498db;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(52, 152, 219, 0.1);
}
.type-item.active {
  border-color: #3498db;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}
.type-item.active .type-icon {
  color: white;
}
.type-icon {
  font-size: 22px;
  color: #3498db;
  display: block;
  margin-bottom: 6px;
  transition: all 0.3s ease;
}
.type-item span {
  font-size: 13px;
  font-weight: 500;
}
/* 登录/注册切换样式 */
.mode-switch {
  text-align: center;
  margin-bottom: 20px;
  font-size: 14px;
  color: #7f8c8d;
}
.mode-switch span {
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 0 10px;
}
.mode-switch span:hover {
  color: #3498db;
}
.mode-switch span.active {
  color: #3498db;
  font-weight: 600;
}
.mode-switch .divider {
  color: #dcdfe6;
  cursor: default;
}
.login-form {
  margin-top: 15px;
}
/* 密码框包裹器样式 */
.password-item {
  position: relative;
}
.password-input-wrapper {
  position: relative;
}
.forgot-password {
  position: absolute;
  right: 0;
  bottom: -20px;
  font-size: 12px;
  color: #3498db;
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 10;
}
.forgot-password:hover {
  color: #2980b9;
  text-decoration: underline;
}
.form-item {
  margin-bottom: 18px;
}
/* 注册模式下的间距 */
.login-form.register-mode .form-item {
  margin-bottom: 6px;
}
.login-form.register-mode .input-label {
  margin-bottom: 3px;
  font-size: 13px;
}
/* 性别单选框样式 */
.el-radio-group {
  width: 100%;
  display: flex;
  gap: 30px;
  padding: 8px 0;
}
.login-form.register-mode .el-radio-group {
  padding: 2px 0;
  margin: 0;
}
.input-label {
  font-size: 14px;
  color: #34495e;
  margin-bottom: 8px;
  font-weight: 500;
}
.custom-input {
  border-radius: 8px;
}
.custom-input >>> .el-input__inner {
  height: 40px;
  border-radius: 8px;
  border: 1px solid #dcdfe6;
  transition: all 0.3s ease;
  padding-left: 40px;
}
.custom-input >>> .el-input__inner:focus {
  border-color: #3498db;
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.1);
}
.custom-input >>> .el-input__prefix {
  left: 12px;
  display: flex;
  align-items: center;
}
.custom-input >>> .el-icon-user,
.custom-input >>> .el-icon-lock,
.custom-input >>> .el-icon-key {
  font-size: 18px;
  color: #7f8c8d;
}
/* 验证码容器样式 */
.captcha-container {
  display: flex;
  gap: 10px;
  align-items: center;
}
.captcha-input {
  flex: 1;
}
.captcha-image {
  cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  transition: all 0.3s ease;
}
.captcha-image:hover {
  border-color: #3498db;
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.1);
}
.captcha-image img {
  display: block;
  width: 120px;
  height: 44px;
}
.login-button {
  width: 100%;
  height: 40px;
  background: #ef744f;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 1px;
  transition: all 0.3s ease;
  margin-top: 8px;
}
.login-form.register-mode .login-button {
  margin-top: 12px;
  margin-bottom: 10px;
}
.login-button:hover {
  background: #2980b9;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.2);
}
.login-button:active {
  transform: translateY(0);
}
.footer-tips {
  margin-top: 20px;
  text-align: center;
}
.tip-text {
  font-size: 13px;
  color: #95a5a6;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.copyright {
  font-size: 12px;
  color: #bdc3c7;
  padding-top: 20px;
  border-top: 1px solid #ecf0f1;
}
/* 找回密码对话框样式 */
.reset-captcha-container {
  display: flex;
  align-items: center;
  width: 100%;
}
</style>
