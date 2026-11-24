<template>
  <div class="dashboard-container">
    <div class="dashboard-header">
      <div class="header-content">
        <div class="welcome-text">
          <h2 class="welcome-title">欢迎回来，{{ staff.name }}</h2>
          <p class="welcome-subtitle">今天是{{ currentDateInfo }}，祝您工作愉快！</p>
        </div>
        <div class="date-badge">
          <i class="el-icon-date"></i>
          <span>{{ currentDateInfo }}</span>
        </div>
      </div>
    </div>

    <el-row class="dashboard-main" :gutter="24">
      <!-- 左侧个人信息区域 -->
      <el-col :span="8">
        <!-- 个人信息卡片 -->
        <el-card class="personal-card card-hover" shadow="hover">
          <div class="personal-info">
            <div class="avatar-section">
              <div class="avatar-wrapper">
                <el-upload
                  class="avatar-upload"
                  :action="uploadApi"
                  :headers="headers"
                  :show-file-list="false"
                  :multiple="false"
                  :on-success="handleUploadSuccess"
                  :limit="1"
                >
                  <img :src="avatar" class="avatar-img" @error="handleAvatarError" />
                  <div class="avatar-overlay">
                    <i class="el-icon-camera"></i>
                  </div>
                </el-upload>
              </div>
              <div class="user-name">
                <h3>{{ staff.name }}</h3>
                <p class="dept-tag">{{ staff.deptName }}</p>
              </div>
            </div>

            <div class="personal-details">
              <div class="detail-item">
                <i class="el-icon-user detail-icon"></i>
                <div class="detail-content">
                  <span class="detail-label">员工编号</span>
                  <span class="detail-value">{{ staff.code }}</span>
                </div>
              </div>
              <div class="detail-item">
                <i class="el-icon-date detail-icon"></i>
                <div class="detail-content">
                  <span class="detail-label">生日</span>
                  <span class="detail-value">{{ staff.birthday }}</span>
                </div>
              </div>
              <div class="detail-item">
                <i class="el-icon-location-outline detail-icon"></i>
                <div class="detail-content">
                  <span class="detail-label">地址</span>
                  <span class="detail-value">{{ staff.address }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 考勤日历卡片 -->
        <el-card class="calendar-card card-hover" shadow="hover">
          <div class="card-header">
            <h3 class="card-title">
              <i class="el-icon-calendar"></i>
              考勤日历
            </h3>
            <el-tag type="info" size="small">{{ attendanceData.date ? attendanceData.date.getMonth() + 1 + '月' : '本月' }}</el-tag>
          </div>
          <el-calendar v-model="attendanceData.date" class="custom-calendar">
            <template slot="dateCell" slot-scope="{date, data}">
              <div class="calendar-day">
                <span class="day-number">{{ date.getDate() }}</span>
                <div v-if="data.type === 'current-month' && attendanceData.list[date.getDate() - 1]"
                     class="attendance-tag">
                  <el-tag
                    :type="attendanceData.list[date.getDate() - 1].tagType"
                    size="mini"
                    effect="plain"
                  >
                    {{ attendanceData.list[date.getDate() - 1].message }}
                  </el-tag>
                </div>
              </div>
            </template>
          </el-calendar>
          <div class="calendar-legend">
            <div class="legend-item">
              <el-tag type="success" size="mini" effect="plain">正常</el-tag>
              <span>出勤</span>
            </div>
            <div class="legend-item">
              <el-tag type="warning" size="mini" effect="plain">迟到</el-tag>
              <span>迟到</span>
            </div>
            <div class="legend-item">
              <el-tag type="danger" size="mini" effect="plain">缺勤</el-tag>
              <span>缺勤</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧数据统计区域 -->
      <el-col :span="16">
        <!-- 数据统计卡片组 -->
        <div class="stats-grid">
          <div v-for="(item,index) in countData" :key="index" class="stats-card card-hover">
            <div class="stats-icon" :style="{ backgroundColor: item.color + '15' }">
              <i :class="'el-icon-' + item.icon" :style="{ color: item.color }"></i>
            </div>
            <div class="stats-content">
              <p class="stats-name">{{ item.name }}</p>
              <p class="stats-value">{{ item.value }}</p>
            </div>
            <div class="stats-trend"></div>
          </div>
        </div>

        <!-- 社保比例图表 -->
        <el-card class="chart-card card-hover" shadow="hover">
          <div class="card-header">
            <h3 class="card-title">
              <i class="el-icon-pie-chart"></i>
              部分城市社保缴费比例
            </h3>
          </div>
          <div class="chart-container">
            <div class="chart-wrapper" ref="city"></div>
          </div>
        </el-card>

        <!-- 图表组合 -->
        <div class="charts-row">
          <el-card class="chart-card-half card-hover" shadow="hover">
            <div class="card-header">
              <h3 class="card-title">
                <i class="el-icon-data-line"></i>
                {{ new Date().getFullYear() }}年员工入职情况
              </h3>
            </div>
            <div class="chart-container">
              <div class="chart-wrapper" ref="staff"></div>
            </div>
          </el-card>

          <el-card class="chart-card-half card-hover" shadow="hover">
            <div class="card-header">
              <h3 class="card-title">
                <i class="el-icon-data-analysis"></i>
                员工分布情况
              </h3>
            </div>
            <div class="chart-container">
              <div class="chart-wrapper" ref="department"></div>
            </div>
          </el-card>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getAttendanceData, getCityData, getCountData, getDepartmentData, getStaffData } from '../../api/home'
import { getDownloadApi, getUploadApi } from '../../api/docs'
import { edit, getInfo } from '../../api/staff'
import { mapState } from 'vuex'

export default {
  name: 'Home',
  data () {
    return {
      dayOfWeek: ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'],
      attendanceData: {
        date: '',
        list: []
      },
      countData: [
        {
          name: '员工总数',
          value: 0,
          icon: 'user',
          color: '#409EFF'
        },
        {
          name: '状态正常',
          value: 0,
          icon: 'success',
          color: '#67C23A'
        },
        {
          name: '状态异常',
          value: 0,
          icon: 'warning',
          color: '#F56C6C'
        },
        {
          name: '今日迟到',
          value: 0,
          icon: 'time',
          color: '#E6A23C'
        },
        {
          name: '今日早退',
          value: 0,
          icon: 'close-notification',
          color: '#909399'
        },
        {
          name: '今日旷工',
          value: 0,
          icon: 'lock',
          color: '#F56C6C'
        }
      ],
      staffOption: {
        title: {
          show: false
        },
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: '#E4E7ED',
          borderWidth: 1,
          textStyle: {
            color: '#606266'
          },
          formatter: function (params) {
            let result = params[0].name + '<br/>'
            params.forEach(item => {
              const icon = item.seriesType === 'line' ? '●' : '■'
              result += `${icon} ${item.seriesName}: ${item.value}人<br/>`
            })
            return result
          }
        },
        legend: {
          bottom: 0,
          icon: 'circle'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '15%',
          top: '12%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: [],
          axisLine: {
            lineStyle: {
              color: '#DCDFE6'
            }
          },
          axisLabel: {
            color: '#606266'
          }
        },
        yAxis: {
          type: 'value',
          name: '人数',
          nameTextStyle: {
            color: '#909399'
          },
          axisLine: {
            lineStyle: {
              color: '#DCDFE6'
            }
          },
          axisLabel: {
            color: '#606266'
          },
          splitLine: {
            lineStyle: {
              color: '#F2F6FC',
              type: 'dashed'
            }
          }
        },
        series: [
          {
            name: '折线图',
            data: [],
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 8,
            itemStyle: {
              color: '#409EFF'
            },
            lineStyle: {
              width: 3
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
                { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
              ])
            }
          },
          {
            name: '柱状图',
            data: [],
            type: 'bar',
            itemStyle: {
              color: '#67C23A'
            },
            barWidth: '40%'
          }
        ]
      },
      cityOption: {
        title: {
          show: false
        },
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: '#E4E7ED',
          borderWidth: 1,
          textStyle: {
            color: '#606266'
          }
        },
        legend: {
          bottom: 0,
          icon: 'circle'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '15%',
          top: '12%',
          containLabel: true
        },
        dataset: {
          source: []
        },
        xAxis: {
          type: 'category',
          axisLine: {
            lineStyle: {
              color: '#DCDFE6'
            }
          },
          axisLabel: {
            color: '#606266'
          }
        },
        yAxis: {
          type: 'value',
          name: '百分比',
          nameTextStyle: {
            color: '#909399'
          },
          axisLabel: {
            formatter: function (val) {
              return (val * 100).toFixed(1) + '%'
            },
            color: '#606266'
          },
          axisLine: {
            lineStyle: {
              color: '#DCDFE6'
            }
          },
          splitLine: {
            lineStyle: {
              color: '#F2F6FC',
              type: 'dashed'
            }
          }
        },
        series: [
          {
            name: '养老保险',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            itemStyle: {
              color: '#409EFF'
            },
            lineStyle: {
              width: 2
            }
          },
          {
            name: '医疗保险',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            itemStyle: {
              color: '#67C23A'
            },
            lineStyle: {
              width: 2
            }
          },
          {
            name: '失业保险',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            itemStyle: {
              color: '#E6A23C'
            },
            lineStyle: {
              width: 2
            }
          },
          {
            name: '生育保险',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            itemStyle: {
              color: '#F56C6C'
            },
            lineStyle: {
              width: 2
            }
          }
        ]
      },
      departmentOption: {
        title: {
          show: false
        },
        tooltip: {
          trigger: 'item',
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: '#E4E7ED',
          borderWidth: 1,
          textStyle: {
            color: '#606266'
          },
          formatter: '{b}: {c}人 ({d}%)'
        },
        legend: {
          orient: 'vertical',
          right: 10,
          top: 'center',
          icon: 'circle'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '3%',
          containLabel: true
        },
        series: [
          {
            name: '部门分布',
            type: 'pie',
            radius: ['40%', '70%'],
            center: ['35%', '50%'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 6,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              show: true,
              position: 'outside',
              formatter: '{b}: {c}人',
              color: '#606266'
            },
            labelLine: {
              length: 10,
              length2: 15,
              smooth: true
            },
            emphasis: {
              label: {
                show: true,
                fontSize: '14',
                fontWeight: 'bold'
              }
            },
            data: [],
            color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#FFE58F']
          }
        ]
      }
    }
  },
  computed: {
    ...mapState('staff', ['staff']),
    ...mapState('token', ['token']),
    loginType () {
      return this.$store.state.staff.loginType
    },
    headers () {
      return { token: this.token }
    },
    uploadApi () {
      return getUploadApi()
    },
    downloadApi () {
      return getDownloadApi()
    },
    currentDateInfo () {
      const date = new Date()
      const year = date.getFullYear()
      const month = date.getMonth() + 1
      const currentDate = date.getDate()
      const day = date.getDay()
      return year + '年' + month + '月' + currentDate + '日 ' + this.dayOfWeek[day]
    },
    avatar () {
      if (this.staff.avatar) {
        return this.downloadApi + this.staff.avatar
      }
      return require('../../assets/images/avatar.png')
    }
  },
  watch: {
    'attendanceData.date':
      function () {
        this.getStaffAttendance()
      }
  },
  methods: {
    // 头像加载失败时使用默认头像
    handleAvatarError () {
      this.$set(this.staff, 'avatar', '')
    },
    loading () {
      getInfo(this.staff.id).then(response => {
        if (response.code === 200) {
          // 保留原有的loginType，避免更新后丢失
          const updatedStaff = {
            ...response.data,
            loginType: this.staff.loginType
          }
          this.$store.commit('staff/SET_STAFF', updatedStaff)
        } else {
          this.$message.error('获取数据失败！')
        }
      })

      getStaffData().then(response => {
        if (response.code === 200) {
          const quarters = ['一季度', '二季度', '三季度', '四季度']
          this.staffOption.xAxis.data = quarters
          this.staffOption.series[0].data = response.data
          this.staffOption.series[1].data = response.data
          const staffChart = echarts.init(this.$refs.staff)
          staffChart.setOption(this.staffOption)

          // 窗口大小变化时重绘图表
          window.addEventListener('resize', () => {
            staffChart.resize()
          })
        } else {
          this.$message.error('获取数据失败！')
        }
      })

      // 获取统计数据
      console.log('当前员工部门ID:', this.staff.deptId, '登录类型:', this.loginType)
      getCountData({
        deptId: this.staff.deptId,
        loginType: this.loginType
      }).then(response => {
        console.log('统计数据返回:', response)
        if (response.code === 200) {
          this.countData[0].value = response.data.totalNum
          this.countData[1].value = response.data.normalNum
          this.countData[2].value = response.data.totalNum - response.data.normalNum
          this.countData[3].value = response.data.lateNum
          this.countData[4].value = response.data.leaveEarlyNum
          this.countData[5].value = response.data.absenteeismNum
        } else {
          this.$message.error('获取数据失败！')
        }
      })

      // 获取城市社保数据
      getCityData().then(response => {
        if (response.code === 200) {
          const cityData = response.data.map(item => [item.name, item.comPensionRate, item.comMedicalRate, item.comUnemploymentRate, item.comMaternityRate])
          cityData.unshift(['product', '养老保险', '医疗保险', '失业保险', '生育保险'])
          this.cityOption.dataset.source = cityData
          const cityChart = echarts.init(this.$refs.city)
          cityChart.setOption(this.cityOption)

          window.addEventListener('resize', () => {
            cityChart.resize()
          })
        } else {
          this.$message.error('获取数据失败！')
        }
      })

      // 获取当前月员工的考勤数据
      getAttendanceData({ id: this.staff.id }).then(response => {
        if (response.code === 200) {
          this.attendanceData.list = response.data
        } else {
          this.$message.error('获取数据失败！')
        }
      })

      getDepartmentData().then(response => {
        if (response.code === 200) {
          this.departmentOption.series[0].data = response.data
          const departmentChart = echarts.init(this.$refs.department)
          departmentChart.setOption(this.departmentOption)

          window.addEventListener('resize', () => {
            departmentChart.resize()
          })
        } else {
          this.$message.error('获取数据失败！')
        }
      })
    },
    // 获取指定月份员工的考勤数据
    getStaffAttendance () {
      const d = this.attendanceData.date
      let month = ''
      if (d.getMonth() < 9) {
        month = d.getFullYear() + '0' + (d.getMonth() + 1)
      } else {
        month = d.getFullYear() + '' + (d.getMonth() + 1)
      }
      getAttendanceData({ id: this.staff.id, month: month }).then(response => {
        if (response.code === 200) {
          this.attendanceData.list = response.data
        } else {
          this.$message.error('获取数据失败！')
        }
      })
    },
    handleUploadSuccess (res) {
      if (res.code === 200) {
        edit({ id: this.staff.id, avatar: res.data.name }).then(response => {
          if (response.code === 200) {
            this.$message.success('头像更新成功！')
            this.$store.commit('staff/SET_AVATAR', res.data.name)
          } else {
            this.$message.error('头像更新失败！')
          }
        })
      } else {
        this.$message.error('文件上传失败！')
      }
    }
  },
  mounted () {
    this.loading()
  },
  beforeDestroy () {
    // 移除窗口大小监听
    window.removeEventListener('resize', () => {})
  }
}
</script>

<style scoped>
.dashboard-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #f8fafc 100%);
  padding: 24px;
}

.dashboard-header {
  margin-bottom: 24px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 12px;
}

.welcome-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
  background: linear-gradient(90deg, #409EFF, #67C23A);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.welcome-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.date-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  font-size: 14px;
  color: #606266;
}

.date-badge i {
  color: #409EFF;
  font-size: 16px;
}

.dashboard-main {
  margin: 0 -12px;
}

/* 卡片悬浮效果 */
.card-hover {
  transition: all 0.3s ease;
  border: 1px solid #EBEEF5;
  border-radius: 12px;
  overflow: hidden;
}

.card-hover:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08) !important;
  border-color: #DCDFE6;
}

/* 个人信息卡片样式 */
.personal-card {
  margin-bottom: 24px;
  background: white;
}

.personal-info {
  padding: 8px;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
}

.avatar-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
}

.avatar-upload {
  width: 100%;
  height: 100%;
  cursor: pointer;
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #F2F6FC;
  transition: all 0.3s ease;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.avatar-wrapper:hover .avatar-img {
  transform: scale(1.05);
}

.avatar-overlay i {
  color: white;
  font-size: 20px;
}

.user-name h3 {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.dept-tag {
  display: inline-block;
  padding: 4px 12px;
  background: #F0F9FF;
  color: #409EFF;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.personal-details {
  background: #F8FAFC;
  border-radius: 8px;
  padding: 16px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #F0F0F0;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border-radius: 6px;
  color: #409EFF;
  font-size: 16px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.detail-content {
  flex: 1;
}

.detail-label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

.detail-value {
  display: block;
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

/* 日历卡片样式 */
.calendar-card {
  background: white;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 0 4px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.card-title i {
  color: #409EFF;
}

.custom-calendar {
  border: none;
}

.custom-calendar >>> .el-calendar-table {
  border-collapse: separate;
  border-spacing: 4px;
}

.custom-calendar >>> .el-calendar-table .el-calendar-day {
  height: 48px;
  padding: 2px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.custom-calendar >>> .el-calendar-table .el-calendar-day:hover {
  background-color: #F5F7FA;
  transform: scale(1.05);
}

.calendar-day {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.day-number {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 2px;
}

.attendance-tag {
  margin-top: 2px;
}

.calendar-legend {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #F0F0F0;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #606266;
}

/* 数据统计卡片样式 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stats-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s ease;
  border: 1px solid #EBEEF5;
}

.stats-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.06) !important;
}

.stats-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stats-icon i {
  font-size: 24px;
}

.stats-content {
  flex: 1;
}

.stats-name {
  font-size: 14px;
  color: #909399;
  margin: 0 0 8px 0;
}

.stats-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

/* 图表卡片样式 */
.chart-card {
  margin-bottom: 24px;
  background: white;
}

.chart-card-half {
  height: 100%;
  background: white;
}

.chart-container {
  position: relative;
  width: 100%;
}

.chart-wrapper {
  width: 100%;
  height: 260px;
}

.charts-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  margin-bottom: 24px;
}

/* 自定义日历表格样式 */
.custom-calendar >>> .el-calendar-table thead th {
  color: #909399;
  font-weight: 500;
  padding: 8px 0;
}

.custom-calendar >>> .el-calendar-table tr:first-child td {
  border-top: none;
}

.custom-calendar >>> .el-calendar-table .el-calendar-day:hover .day-number {
  color: #409EFF;
}
</style>
