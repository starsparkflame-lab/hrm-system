<template>
  <el-form label-width="100px" size="mini">
    <el-form-item label="选择月份">
      <el-date-picker
        v-model="selectedMonth"
        type="month"
        value-format="yyyyMM"
        placeholder="选择月份"
        @change="loadCalendar"
      />
    </el-form-item>

    <div class="calendar-container" v-loading="loading">
      <div class="calendar-header">
        <span class="weekday" v-for="day in weekDays" :key="day">{{ day }}</span>
      </div>

      <div class="calendar-body">
        <!-- 空白占位 -->
        <div
          v-for="n in firstDayOffset"
          :key="'empty-' + n"
          class="calendar-day empty"
        ></div>

        <!-- 日期格子 -->
        <div
          v-for="(day, index) in calendarDays"
          :key="index"
          class="calendar-day"
          :class="[
            day.isWorkday !== undefined ? (day.isWorkday ? 'workday' : 'holiday') : '',
            day.isToday ? 'today' : '',
            getStatusClass(day)
          ]"
          @click="toggleWorkday(day)"
        >
          <div class="day-number">{{ day.date }}</div>
          <div class="day-status">
            <el-tag
              v-if="day.isWorkday !== undefined"
              size="mini"
              :type="getStatusType(day)"
            >
              {{ getStatusText(day) }}
            </el-tag>
            <el-select
              v-if="day.isWorkday === 1"
              v-model="day.expectedStatus"
              size="mini"
              @click.stop
              style="margin-top: 5px; width: 100%"
              placeholder="状态"
            >
              <el-option label="正常" :value="1"></el-option>
              <el-option label="迟到" :value="2"></el-option>
              <el-option label="早退" :value="3"></el-option>
              <el-option label="缺勤" :value="4"></el-option>
            </el-select>
            <el-input
              v-if="day.isWorkday === 0"
              v-model="day.remark"
              size="mini"
              placeholder="备注"
              @click.stop
              style="margin-top: 5px; width: 100%"
            />
          </div>
        </div>
      </div>
    </div>

    <div class="calendar-footer">
      <el-button type="primary" size="small" @click="saveCalendar">保存设置</el-button>
      <el-button size="small" @click="resetCalendar">重置</el-button>
    </div>

    <div class="legend">
      <span><i class="status-dot workday"></i> 上班</span>
      <span><i class="status-dot holiday"></i> 休假</span>
      <span><i class="status-dot weekend"></i> 周末</span>
      <span><i class="status-dot today"></i> 今天</span>
    </div>
  </el-form>
</template>

<script>
import { batchSaveDeptCalendar, getDeptMonthCalendar } from '@/api/deptCalendar'

export default {
  name: 'WorkCalendarSetting',
  props: {
    deptId: {
      type: Number,
      required: true
    }
  },
  data () {
    return {
      selectedMonth: '',
      loading: false,
      calendarDays: [],
      weekDays: ['一', '二', '三', '四', '五', '六', '日'],
      originalData: [],
      statusMap: {
        1: { text: '正常', type: 'success', class: 'status-normal' },
        2: { text: '迟到', type: 'warning', class: 'status-late' },
        3: { text: '早退', type: 'warning', class: 'status-early' },
        4: { text: '缺勤', type: 'danger', class: 'status-absent' },
        5: { text: '休假', type: 'info', class: 'status-holiday' }
      }
    }
  },
  computed: {
    firstDayOffset () {
      if (this.calendarDays.length === 0) return 0
      const firstDay = this.calendarDays[0]
      const date = new Date(firstDay.fullDate)
      const dayOfWeek = date.getDay()
      return dayOfWeek === 0 ? 6 : dayOfWeek - 1
    }
  },
  methods: {
    init () {
      // 默认当前月份
      const now = new Date()
      this.selectedMonth = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}`
      this.loadCalendar()
    },
    async loadCalendar () {
      if (!this.deptId) {
        console.log('WorkCalendarSetting: deptId为空')
        this.$message.warning('请先选择部门')
        return
      }

      console.log('WorkCalendarSetting: 开始加载日历, deptId=', this.deptId, 'month=', this.selectedMonth)
      this.loading = true
      try {
        const response = await getDeptMonthCalendar({
          deptId: this.deptId,
          yearMonth: this.selectedMonth
        })

        console.log('WorkCalendarSetting: API响应', response)

        if (response.code === 200) {
          this.calendarDays = this.generateCalendarDays(response.data.calendarList || [])
          this.originalData = JSON.parse(JSON.stringify(this.calendarDays))
          console.log('WorkCalendarSetting: 日历数据生成成功', this.calendarDays.length, '天')
        } else {
          console.error('WorkCalendarSetting: API返回错误', response)
          this.$message.error(response.message || '加载失败')
        }
      } catch (error) {
        console.error('WorkCalendarSetting: 加载异常', error)
        this.$message.error('加载日历数据失败: ' + (error.message || '未知错误'))
      } finally {
        this.loading = false
      }
    },
    generateCalendarDays (existingData) {
      const year = parseInt(this.selectedMonth.substring(0, 4))
      const month = parseInt(this.selectedMonth.substring(4, 6))
      const daysInMonth = new Date(year, month, 0).getDate()
      const days = []

      for (let day = 1; day <= daysInMonth; day++) {
        const date = new Date(year, month - 1, day)
        const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
        const existing = existingData.find(item => item.workDate === dateStr)

        days.push({
          date: day,
          fullDate: dateStr,
          isWorkday: existing ? existing.isWorkday : (date.getDay() >= 1 && date.getDay() <= 5 ? 1 : 0),
          expectedStatus: existing ? existing.expectedStatus : 1,
          remark: existing ? existing.remark : null,
          id: existing ? existing.id : null,
          isToday: this.isToday(date)
        })
      }

      return days
    },
    isToday (date) {
      const today = new Date()
      return date.getFullYear() === today.getFullYear() &&
             date.getMonth() === today.getMonth() &&
             date.getDate() === today.getDate()
    },
    toggleWorkday (day) {
      if (day.isWorkday === undefined) return

      // 切换状态
      day.isWorkday = day.isWorkday === 1 ? 0 : 1

      // 如果切换为休假,清空备注
      if (day.isWorkday === 0 && !day.remark) {
        day.remark = ''
        day.expectedStatus = 5 // 设置为休假状态
      }

      // 如果切换为上班,设置默认预期状态
      if (day.isWorkday === 1 && !day.expectedStatus) {
        day.expectedStatus = 1 // 默认为正常
      }
    },
    async saveCalendar () {
      if (!this.deptId) {
        this.$message.warning('请先选择部门')
        return
      }

      try {
        const data = this.calendarDays
          .filter(day => day.isWorkday !== undefined)
          .map(day => ({
            id: day.id,
            deptId: this.deptId,
            workDate: day.fullDate,
            isWorkday: day.isWorkday,
            expectedStatus: day.isWorkday === 1 ? day.expectedStatus : 5,
            remark: day.isWorkday === 0 ? day.remark : null
          }))

        const response = await batchSaveDeptCalendar(data)
        if (response.code === 200) {
          this.$message.success('保存成功')
          this.loadCalendar()
        } else {
          this.$message.error(response.message || '保存失败')
        }
      } catch (error) {
        this.$message.error('保存失败')
      }
    },
    resetCalendar () {
      this.calendarDays = JSON.parse(JSON.stringify(this.originalData))
      this.$message.info('已重置')
    },
    getStatusText (day) {
      if (day.isWorkday === 0) {
        return '休假'
      }
      const status = this.statusMap[day.expectedStatus]
      return status ? status.text : '上班'
    },
    getStatusType (day) {
      if (day.isWorkday === 0) {
        return 'info'
      }
      const status = this.statusMap[day.expectedStatus]
      return status ? status.type : 'success'
    },
    getStatusClass (day) {
      if (day.isWorkday === 0) {
        return 'status-holiday'
      }
      const status = this.statusMap[day.expectedStatus]
      return status ? status.class : ''
    }
  },
  watch: {
    deptId: {
      handler (newVal) {
        if (newVal) {
          this.init()
        }
      },
      immediate: true
    }
  }
}
</script>

<style lang="less" scoped>
.calendar-container {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 10px;
  margin-top: 10px;

  .calendar-header {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 5px;
    margin-bottom: 5px;

    .weekday {
      text-align: center;
      padding: 8px;
      font-weight: bold;
      background-color: #f5f7fa;
      border-radius: 4px;
      font-size: 12px;
    }
  }

  .calendar-body {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 5px;

    .calendar-day {
      min-height: 80px;
      padding: 5px;
      border: 1px solid #e4e7ed;
      border-radius: 4px;
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        transform: translateY(-2px);
      }

      &.empty {
        border: none;
        background: none;
        cursor: default;

        &:hover {
          box-shadow: none;
          transform: none;
        }
      }

      &.workday {
        background-color: #f0f9ff;
        border-color: #67c23a;
      }

      &.holiday {
        background-color: #fef0f0;
        border-color: #f56c6c;
      }

      &.status-normal {
        background-color: #f0f9ff;
        border-color: #67c23a;
      }

      &.status-late {
        background-color: #fdf6ec;
        border-color: #e6a23c;
      }

      &.status-early {
        background-color: #fdf6ec;
        border-color: #e6a23c;
      }

      &.status-absent {
        background-color: #fef0f0;
        border-color: #f56c6c;
      }

      &.status-holiday {
        background-color: #f4f4f5;
        border-color: #909399;
      }

      &.today {
        border-color: #409eff;
        border-width: 2px;
        box-shadow: 0 0 5px rgba(64, 158, 255, 0.5);
      }

      .day-number {
        font-size: 14px;
        font-weight: bold;
        margin-bottom: 5px;
        text-align: center;
      }

      .day-status {
        text-align: center;
      }
    }
  }
}

.calendar-footer {
  margin-top: 15px;
  text-align: center;
}

.legend {
  margin-top: 15px;
  display: flex;
  justify-content: center;
  gap: 20px;
  font-size: 12px;

  .status-dot {
    display: inline-block;
    width: 12px;
    height: 12px;
    border-radius: 50%;
    margin-right: 5px;
    vertical-align: middle;

    &.workday {
      background-color: #67c23a;
    }

    &.holiday {
      background-color: #f56c6c;
    }

    &.weekend {
      background-color: #909399;
    }

    &.today {
      background-color: #409eff;
      border: 2px solid #fff;
      box-shadow: 0 0 3px rgba(64, 158, 255, 0.5);
    }
  }
}
</style>
