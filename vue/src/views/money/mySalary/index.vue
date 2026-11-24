<template>
  <div>
    <!-- 搜索区域 -->
    <div style="margin-bottom: 20px;">
      <el-form :inline="true">
        <el-form-item label="月份">
          <el-date-picker
            v-model="queryForm.month"
            type="month"
            placeholder="选择月份"
            value-format="yyyyMM"
            style="width: 200px"
          ></el-date-picker>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-alert
        title="提示:请选择要查看的月份,默认显示当前月"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 10px"
      ></el-alert>
    </div>

    <!-- 工资表格 -->
    <el-table :data="tableData" border stripe style="width: 100%">
      <el-table-column prop="deptName" label="部门" width="120"></el-table-column>
      <el-table-column prop="name" label="姓名" width="100"></el-table-column>
      <el-table-column prop="month" label="月份" width="100" align="center"></el-table-column>

      <el-table-column label="扣款项目" align="center">
        <el-table-column prop="lateDeduct" label="迟到" width="80" align="center"></el-table-column>
        <el-table-column prop="leaveEarlyDeduct" label="早退" width="80" align="center"></el-table-column>
        <el-table-column prop="absenteeismDeduct" label="旷工" width="80" align="center"></el-table-column>
        <el-table-column prop="leaveDeduct" label="请假" width="80" align="center"></el-table-column>
      </el-table-column>

      <el-table-column label="缴纳项目" align="center">
        <el-table-column prop="housePay" label="公积金" width="100" align="center"></el-table-column>
        <el-table-column prop="socialPay" label="社保" width="100" align="center"></el-table-column>
      </el-table-column>

      <el-table-column label="实发工资" align="center">
        <el-table-column prop="baseSalary" label="基础工资" width="100" align="center"></el-table-column>
        <el-table-column prop="subsidy" label="生活补贴" width="100" align="center"></el-table-column>
        <el-table-column prop="bonus" label="奖金" width="100" align="center"></el-table-column>
        <el-table-column prop="totalSalary" label="实发工资" width="120" align="center">
          <template slot-scope="scope">
            <span style="color: #67C23A; font-weight: bold;">¥{{ scope.row.totalSalary ? scope.row.totalSalary.toFixed(2) : '0.00' }}</span>
          </template>
        </el-table-column>
      </el-table-column>

      <el-table-column prop="remark" label="备注" min-width="150"></el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      style="margin-top: 20px; text-align: right"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page="queryForm.current"
      :page-sizes="[5, 10, 20, 50]"
      :page-size="queryForm.size"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
    ></el-pagination>
  </div>
</template>

<script>
import { listMySalary } from '@/api/mySalary'

export default {
  name: 'MySalary',
  data () {
    return {
      queryForm: {
        current: 1,
        size: 10,
        month: '',
        staffId: ''
      },
      tableData: [],
      total: 0
    }
  },
  created () {
    // 设置默认月份为当前月
    const now = new Date()
    const year = now.getFullYear()
    const month = String(now.getMonth() + 1).padStart(2, '0')
    this.queryForm.month = `${year}${month}`

    // 从Vuex store或localStorage获取员工信息
    const staff = this.$store.state.staff.staff || JSON.parse(localStorage.getItem('staff') || '{}')
    const loginType = this.$store.state.staff.loginType || localStorage.getItem('loginType')

    console.log('当前员工信息:', staff)
    console.log('登录类型:', loginType)
    console.log('查询月份:', this.queryForm.month)

    // 管理员可以看到所有人的工资,普通员工只能看到自己的
    if (loginType === 'admin') {
      this.queryForm.staffId = null // 管理员不传staffId,查询所有
    } else {
      this.queryForm.staffId = staff.id // 普通员工传自己的ID
    }

    console.log('查询的staffId:', this.queryForm.staffId)

    if (loginType !== 'admin' && !this.queryForm.staffId) {
      this.$message.error('无法获取员工信息,请重新登录')
      return
    }

    this.fetchData()
  },
  methods: {
    fetchData () {
      console.log('查询参数:', this.queryForm)
      listMySalary(this.queryForm).then(res => {
        console.log('后端返回数据:', res)
        // 后端返回的code是数字200,不是字符串'0'
        if (res.code === 200) {
          this.tableData = res.data.list || []
          this.total = res.data.total || 0
          console.log('工资数据:', this.tableData)
          if (this.tableData.length === 0) {
            this.$message.info('暂无工资数据')
          }
        } else {
          this.$message.error(res.message || '获取工资数据失败')
        }
      }).catch(err => {
        console.error('请求错误:', err)
        this.$message.error('请求失败,请检查网络连接')
      })
    },
    handleSearch () {
      this.queryForm.current = 1
      this.fetchData()
    },
    handleReset () {
      this.queryForm.month = ''
      this.handleSearch()
    },
    handleSizeChange (val) {
      this.queryForm.size = val
      this.fetchData()
    },
    handleCurrentChange (val) {
      this.queryForm.current = val
      this.fetchData()
    }
  }
}
</script>

<style scoped>
</style>
