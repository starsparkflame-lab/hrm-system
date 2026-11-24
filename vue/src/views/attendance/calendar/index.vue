<template>
  <div class="calendar-container">
    <!-- 月份选择器 -->
    <div class="header">
      <el-date-picker
        v-model="selectedMonth"
        type="month"
        value-format="yyyyMM"
        placeholder="选择月份"
        @change="handleMonthChange"
      />
    </div>

    <!-- 部门选择器(仅管理员显示) -->
    <div class="dept-selector" v-if="isAdmin">
      <el-radio-group v-model="selectedDept" @change="handleDeptChange">
        <el-radio-button label="0">全部部门</el-radio-button>
        <el-radio-button
          v-for="dept in deptList"
          :key="dept.id"
          :label="dept.id"
        >
          {{ dept.name }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- 日历展示 -->
    <div class="calendar-content" v-loading="loading">
      <el-card
        v-for="dept in displayDeptList"
        :key="dept.deptId"
        class="dept-calendar"
      >
        <div slot="header" class="dept-header">
          <span class="dept-name">{{ dept.deptName }}</span>
          <span class="dept-stats">
            正常: <el-tag size="mini" type="success">{{ getDeptStats(dept).normal }}</el-tag>
            迟到: <el-tag size="mini" type="">{{ getDeptStats(dept).late }}</el-tag>
            早退: <el-tag size="mini" type="warning">{{ getDeptStats(dept).leaveEarly }}</el-tag>
            缺勤: <el-tag size="mini" type="danger">{{ getDeptStats(dept).absenteeism }}</el-tag>
            休假: <el-tag size="mini" type="info">{{ getDeptStats(dept).leave }}</el-tag>
          </span>
        </div>

        <div class="calendar-grid">
          <!-- 星期头 -->
          <div class="calendar-weekday" v-for="day in weekDays" :key="day">{{ day }}</div>

          <!-- 空白占位(月初前的空白天数) -->
          <div
            v-for="n in firstDayOffset"
            :key="'empty-' + n"
            class="calendar-day empty"
          ></div>

          <!-- 日期格子 -->
          <div
            v-for="(dayData, index) in dept.calendarData"
            :key="index"
            class="calendar-day"
            :class="[dayData.tagType ? 'has-status' : '', dayData.tagType]"
            @click="showDayDetail(dayData, dept.deptName)"
          >
            <div class="day-number">{{ getDayNumber(dayData.date) }}</div>
            <div class="day-status" v-if="dayData.message">
              <el-tag size="mini" :type="dayData.tagType" effect="plain">
                {{ dayData.message }}
              </el-tag>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 日期详情对话框 -->
    <el-dialog
      title="考勤详情"
      :visible.sync="dialogVisible"
      width="600px"
    >
      <div class="detail-content">
        <h3>{{ dialogData.deptName }} - {{ formatDate(dialogData.date) }}</h3>
        <el-divider />
        <div class="stats-grid">
          <div class="stat-item">
            <span class="label">总人数:</span>
            <span class="value">{{ dialogData.totalCount || 0 }}</span>
          </div>
          <div class="stat-item">
            <span class="label">正常:</span>
            <span class="value success">{{ dialogData.normalCount || 0 }}</span>
          </div>
          <div class="stat-item">
            <span class="label">迟到:</span>
            <span class="value">{{ dialogData.lateCount || 0 }}</span>
          </div>
          <div class="stat-item">
            <span class="label">早退:</span>
            <span class="value warning">{{ dialogData.leaveEarlyCount || 0 }}</span>
          </div>
          <div class="stat-item">
            <span class="label">缺勤:</span>
            <span class="value danger">{{ dialogData.absenteeismCount || 0 }}</span>
          </div>
          <div class="stat-item">
            <span class="label">休假:</span>
            <span class="value info">{{ dialogData.leaveCount || 0 }}</span>
          </div>
        </div>
      </div>
      <div slot="footer">
        <el-button @click="dialogVisible = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getDeptCalendar } from '@/api/calendar'
import { getAllDept } from '@/api/dept'
import { mapState } from 'vuex'

export default {
  name: 'WorkCalendar',
  data () {
    return {
      selectedMonth: '',
      selectedDept: 0,
      deptList: [],
      calendarData: [],
      loading: false,
      dialogVisible: false,
      dialogData: {},
      weekDays: ['一', '二', '三', '四', '五', '六', '日']
    }
  },
  computed: {
    ...mapState('staff', ['staff']),
    userInfo () {
      return this.staff
    },
    isAdmin () {
      return this.userInfo?.loginType === 'admin'
    },
    displayDeptList () {
      return this.calendarData
    },
    firstDayOffset () {
      if (this.calendarData.length === 0) return 0
      const firstDay = this.calendarData[0]?.calendarData[0]?.date
      if (!firstDay) return 0
      const dayOfWeek = new Date(firstDay).getDay()
      return dayOfWeek === 0 ? 6 : dayOfWeek - 1
    }
  },
  methods: {
    async loadCalendarData () {
      this.loading = true
      try {
        const params = {
          month: this.selectedMonth
        }

        console.log('[DEBUG] 当前用户信息:', this.userInfo)
        console.log('[DEBUG] isAdmin:', this.isAdmin)
        console.log('[DEBUG] selectedDept:', this.selectedDept)

        // 如果选择了特定部门且不是管理员,传部门ID
        if (!this.isAdmin && this.userInfo?.deptId) {
          params.deptId = this.userInfo.deptId
          console.log('[DEBUG] 员工查看自己部门:', params.deptId)
        } else if (this.selectedDept > 0) {
          params.deptId = this.selectedDept
          console.log('[DEBUG] 管理员查看指定部门:', params.deptId)
        } else {
          console.log('[DEBUG] 管理员查看全部部门')
        }

        console.log('[DEBUG] 请求参数:', params)
        const response = await getDeptCalendar(params)
        console.log('[DEBUG] 后端返回数据:', response.data)

        if (response.code === 200) {
          this.calendarData = response.data.deptList || []
          console.log('[DEBUG] 渲染的部门数量:', this.calendarData.length)
        } else {
          this.$message.error(response.message || '加载失败')
        }
      } catch (error) {
        this.$message.error('加载日历数据失败')
      } finally {
        this.loading = false
      }
    },
    async loadDeptList () {
      try {
        const response = await getAllDept()
        if (response.code === 200) {
          // 过滤出子部门
          this.deptList = []
          response.data.forEach(dept => {
            if (dept.children && dept.children.length > 0) {
              dept.children.forEach(child => {
                this.deptList.push(child)
              })
            }
          })
        }
      } catch (error) {
        console.error('加载部门列表失败', error)
      }
    },
    handleMonthChange () {
      this.loadCalendarData()
    },
    handleDeptChange () {
      this.loadCalendarData()
    },
    getDayNumber (date) {
      if (!date) return ''
      return new Date(date).getDate()
    },
    formatDate (date) {
      if (!date) return ''
      const d = new Date(date)
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    },
    showDayDetail (dayData, deptName) {
      this.dialogData = {
        ...dayData,
        deptName
      }
      this.dialogVisible = true
    },
    getDeptStats (dept) {
      const stats = {
        normal: 0,
        late: 0,
        leaveEarly: 0,
        absenteeism: 0,
        leave: 0
      }

      if (!dept.calendarData) return stats

      dept.calendarData.forEach(day => {
        stats.normal += day.normalCount || 0
        stats.late += day.lateCount || 0
        stats.leaveEarly += day.leaveEarlyCount || 0
        stats.absenteeism += day.absenteeismCount || 0
        stats.leave += day.leaveCount || 0
      })

      return stats
    }
  },
  created () {
    // 默认当前月份
    const now = new Date()
    this.selectedMonth = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}`

    this.loadDeptList()
    this.loadCalendarData()
  }
}
</script>

<style lang="less" scoped>
.calendar-container {
  padding: 20px;

  .header {
    margin-bottom: 20px;
  }

  .dept-selector {
    margin-bottom: 20px;
  }

  .calendar-content {
    .dept-calendar {
      margin-bottom: 20px;

      .dept-header {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .dept-name {
          font-size: 16px;
          font-weight: bold;
        }

        .dept-stats {
          font-size: 12px;

          .el-tag {
            margin: 0 3px;
          }
        }
      }

      .calendar-grid {
        display: grid;
        grid-template-columns: repeat(7, 1fr);
        gap: 5px;

        .calendar-weekday {
          text-align: center;
          padding: 10px;
          font-weight: bold;
          background-color: #f5f7fa;
          border-radius: 4px;
        }

        .calendar-day {
          min-height: 80px;
          padding: 5px;
          border: 1px solid #e4e7ed;
          border-radius: 4px;
          cursor: pointer;
          transition: all 0.3s;

          &:hover {
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
          }

          &.empty {
            border: none;
            background: none;
            cursor: default;
          }

          &.has-status {
            &.success {
              background-color: #f0f9ff;
              border-color: #67c23a;
            }

            &.warning {
              background-color: #fdf6ec;
              border-color: #e6a23c;
            }

            &.danger {
              background-color: #fef0f0;
              border-color: #f56c6c;
            }

            &.info {
              background-color: #f4f4f5;
              border-color: #909399;
            }
          }

          .day-number {
            font-size: 14px;
            font-weight: bold;
            margin-bottom: 5px;
          }

          .day-status {
            text-align: center;
          }
        }
      }
    }
  }

  .detail-content {
    h3 {
      margin: 0 0 10px 0;
      text-align: center;
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 15px;

      .stat-item {
        display: flex;
        justify-content: space-between;
        padding: 10px;
        background-color: #f5f7fa;
        border-radius: 4px;

        .label {
          font-weight: bold;
        }

        .value {
          &.success {
            color: #67c23a;
          }

          &.warning {
            color: #e6a23c;
          }

          &.danger {
            color: #f56c6c;
          }

          &.info {
            color: #909399;
          }
        }
      }
    }
  }
}
</style>
