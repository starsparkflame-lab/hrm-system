<template>
  <div class="header-container">
    <div class="header-left">
      <el-button
        type="text"
        icon="el-icon-s-fold"
        @click="$emit('toggle-sidebar')"
        style="font-size: 20px; color: #333;"
      ></el-button>
    </div>
    <div class="header-right">
      <el-dropdown @command="handleCommand">
        <div class="user-info">
          <el-avatar :size="32" :src="avatar" class="user-avatar">
            <i class="el-icon-user-solid"></i>
          </el-avatar>
          <span class="user-name">{{ staff.name }}</span>
          <i class="el-icon-arrow-down" style="margin-left: 5px;"></i>
        </div>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="password">修改密码</el-dropdown-item>
          <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>

    <!-- 修改密码对话框 -->
    <el-dialog
      title="修改密码"
      :visible.sync="passwordDialogVisible"
      width="50%"
      :close-on-click-modal="false"
    >
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordForm" label-width="100px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入旧密码"
            show-password
          ></el-input>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          ></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          ></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="passwordDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleUpdatePassword">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { mapState } from 'vuex'
import { checkPassword, updatePassword } from '../api/staff'
import { logout as logoutApi } from '../api/login'
import { getUploadApi } from '../api/docs'

export default {
  name: 'CommonHeader',
  props: {
    isCollapse: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      passwordDialogVisible: false,
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      passwordRules: {
        oldPassword: [
          { required: true, message: '请输入旧密码', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入新密码', trigger: 'blur' },
          {
            validator: (rule, value, callback) => {
              if (value !== this.passwordForm.newPassword) {
                callback(new Error('两次输入密码不一致'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }
        ]
      }
    }
  },
  computed: {
    ...mapState('staff', ['staff']),
    avatar () {
      return this.staff && this.staff.avatar
        ? getUploadApi().replace('/upload', '/download/' + this.staff.avatar)
        : ''
    }
  },
  methods: {
    handleCommand (command) {
      if (command === 'logout') {
        this.$confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          // 先通知服务端撤销 token 白名单，接口异常也不能阻塞本地登出
          logoutApi().catch(() => {}).then(() => {
            this.$store.dispatch('staff/logout')
          })
        }).catch(() => {})
      } else if (command === 'password') {
        this.passwordDialogVisible = true
        this.passwordForm = {
          oldPassword: '',
          newPassword: '',
          confirmPassword: ''
        }
      }
    },
    handleUpdatePassword () {
      this.$refs.passwordForm.validate(async (valid) => {
        if (!valid) return
        try {
          // 先验证旧密码
          const checkRes = await checkPassword(this.passwordForm.oldPassword, this.staff.id)
          if (checkRes.code !== 200) {
            this.$message.error('旧密码错误')
            return
          }
          // 更新密码
          const updateRes = await updatePassword({
            id: this.staff.id,
            password: this.passwordForm.newPassword
          })
          if (updateRes.code === 200) {
            this.$message.success('密码修改成功，请重新登录')
            this.passwordDialogVisible = false
            this.$store.dispatch('staff/logout')
          } else {
            this.$message.error(updateRes.message || '密码修改失败')
          }
        } catch (error) {
          this.$message.error('密码修改失败')
        }
      })
    }
  }
}
</script>

<style lang="less" scoped>
.header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 4px;
  transition: background-color 0.3s;

  &:hover {
    background-color: rgba(0, 0, 0, 0.05);
  }
}

.user-avatar {
  margin-right: 8px;
}

.user-name {
  font-size: 14px;
  color: #333;
}
</style>
