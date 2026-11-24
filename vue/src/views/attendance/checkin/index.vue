<template>
  <div style="padding: 20px;">
    <el-card>
      <div slot="header">
        <span style="font-size: 18px; font-weight: bold;">📅 考勤签到</span>
      </div>

      <!-- 签到状态 -->
      <el-row :gutter="20" style="margin-bottom: 30px;">
        <el-col :span="6">
          <el-card :body-style="{ padding: '20px' }" :class="status.morningWork ? 'checked-card' : 'unchecked-card'">
            <div style="text-align: center;">
              <i :class="status.morningWork ? 'el-icon-success' : 'el-icon-warning'"
                 :style="{ fontSize: '40px', color: status.morningWork ? '#67C23A' : '#E6A23C' }"></i>
              <div style="margin-top: 10px; font-size: 16px;">上午上班</div>
              <div style="margin-top: 5px; color: #909399;">{{ dept.morStartTime || '--:--' }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card :body-style="{ padding: '20px' }" :class="status.morningOff ? 'checked-card' : 'unchecked-card'">
            <div style="text-align: center;">
              <i :class="status.morningOff ? 'el-icon-success' : 'el-icon-warning'"
                 :style="{ fontSize: '40px', color: status.morningOff ? '#67C23A' : '#E6A23C' }"></i>
              <div style="margin-top: 10px; font-size: 16px;">上午下班</div>
              <div style="margin-top: 5px; color: #909399;">{{ dept.morEndTime || '--:--' }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card :body-style="{ padding: '20px' }" :class="status.afternoonWork ? 'checked-card' : 'unchecked-card'">
            <div style="text-align: center;">
              <i :class="status.afternoonWork ? 'el-icon-success' : 'el-icon-warning'"
                 :style="{ fontSize: '40px', color: status.afternoonWork ? '#67C23A' : '#E6A23C' }"></i>
              <div style="margin-top: 10px; font-size: 16px;">下午上班</div>
              <div style="margin-top: 5px; color: #909399;">{{ dept.aftStartTime || '--:--' }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card :body-style="{ padding: '20px' }" :class="status.afternoonOff ? 'checked-card' : 'unchecked-card'">
            <div style="text-align: center;">
              <i :class="status.afternoonOff ? 'el-icon-success' : 'el-icon-warning'"
                 :style="{ fontSize: '40px', color: status.afternoonOff ? '#67C23A' : '#E6A23C' }"></i>
              <div style="margin-top: 10px; font-size: 16px;">下午下班</div>
              <div style="margin-top: 5px; color: #909399;">{{ dept.aftEndTime || '--:--' }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 签到按钮 -->
      <div style="text-align: center; margin: 30px 0;">
        <el-button type="primary" size="large" icon="el-icon-camera" @click="handleCheckin(1)"
                   :disabled="status.morningWork" style="margin: 10px;">
          上午上班签到
        </el-button>
        <el-button type="success" size="large" icon="el-icon-camera" @click="handleCheckin(2)"
                   :disabled="status.morningOff" style="margin: 10px;">
          上午下班签到
        </el-button>
        <el-button type="warning" size="large" icon="el-icon-camera" @click="handleCheckin(3)"
                   :disabled="status.afternoonWork" style="margin: 10px;">
          下午上班签到
        </el-button>
        <el-button type="danger" size="large" icon="el-icon-camera" @click="handleCheckin(4)"
                   :disabled="status.afternoonOff" style="margin: 10px;">
          下午下班签到
        </el-button>
      </div>

      <!-- 今日签到记录 -->
      <el-table :data="todayRecords" border stripe style="margin-top: 30px;">
        <el-table-column prop="checkinType" label="签到类型" width="120" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.checkinType === 1" type="primary">上午上班</el-tag>
            <el-tag v-else-if="scope.row.checkinType === 2" type="success">上午下班</el-tag>
            <el-tag v-else-if="scope.row.checkinType === 3" type="warning">下午上班</el-tag>
            <el-tag v-else type="danger">下午下班</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="checkinTime" label="签到时间" width="120" align="center"></el-table-column>
        <el-table-column prop="location" label="签到位置" min-width="200"></el-table-column>
        <el-table-column prop="createTime" label="记录时间" width="180" align="center"></el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { doCheckin, getTodayCheckins, getCheckinStatus, getDeptWorkTime } from '@/api/checkin'

export default {
  name: 'Checkin',
  data () {
    return {
      staffId: '',
      status: {
        morningWork: false,
        morningOff: false,
        afternoonWork: false,
        afternoonOff: false
      },
      dept: {
        morStartTime: '',
        morEndTime: '',
        aftStartTime: '',
        aftEndTime: ''
      },
      todayRecords: []
    }
  },
  created () {
    const staff = this.$store.state.staff.staff || JSON.parse(localStorage.getItem('staff') || '{}')
    this.staffId = staff.id
    this.fetchDeptTime()
    this.fetchStatus()
    this.fetchTodayRecords()
  },
  methods: {
    // 获取部门作息时间
    fetchDeptTime () {
      getDeptWorkTime(this.staffId).then(res => {
        if (res.code === 200) {
          this.dept = res.data
        }
      })
    },
    // 获取签到状态
    fetchStatus () {
      getCheckinStatus(this.staffId).then(res => {
        if (res.code === 200) {
          this.status = res.data
        }
      })
    },
    // 获取今日签到记录
    fetchTodayRecords () {
      getTodayCheckins(this.staffId).then(res => {
        if (res.code === 200) {
          this.todayRecords = res.data
        }
      })
    },
    // 签到
    handleCheckin (type) {
      const typeNames = {
        1: '上午上班',
        2: '上午下班',
        3: '下午上班',
        4: '下午下班'
      }

      this.$confirm(`确认进行${typeNames[type]}签到吗?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        doCheckin({
          staffId: this.staffId,
          checkinType: type,
          location: '公司'
        }).then(res => {
          if (res.code === 200) {
            this.$message.success(res.message)
            this.fetchStatus()
            this.fetchTodayRecords()
          } else {
            this.$message.error(res.message)
          }
        })
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.checked-card {
  border: 2px solid #67C23A;
}
.unchecked-card {
  border: 2px solid #E6A23C;
}
</style>
