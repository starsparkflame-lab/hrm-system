<template>
  <div class="manage">
    <!-- 操作按钮 -->
    <div style="margin-bottom: 10px">
      <el-button type="primary" size="mini" @click="handleAdd"
      >新增请假 <i class="el-icon-plus"></i>
      </el-button>
    </div>

    <!-- 数据表格 -->
    <div class="common-table">
      <el-table
        :data="table.tableData"
        height="85%"
        border
        stripe
        row-key="id"
        :header-cell-style="{background: '#eef1f6',color: '#606266',
        textAlign:'center',fontWeight:'bold',borderWidth:'3px'}"
      >
        <el-table-column prop="staffLeave.id" label="ID" min-width="60" align="center"/>
        <el-table-column prop="staffLeave.createTime" label="申请时间" min-width="180" align="center"/>
        <el-table-column label="请假类型" min-width="120" align="center">
          <template slot-scope="scope">
            {{ getLeaveTypeName(scope.row.staffLeave.typeNum) }}
          </template>
        </el-table-column>
        <el-table-column prop="staffLeave.startDate" label="开始日期" min-width="125" align="center"/>
        <el-table-column prop="staffLeave.endDate" label="结束日期" min-width="125" align="center"/>
        <el-table-column prop="staffLeave.days" label="天数" min-width="80" align="center"/>
        <el-table-column label="审核状态" min-width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.tagType">{{ getStatusName(scope.row.staffLeave.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="200" prop="staffLeave.remark" align="center"/>
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button size="mini" type="danger" @click="handleCancel(scope.row)"
                       :disabled="scope.row.staffLeave.status!==scope.row.unaudited"
            >撤销 <i class="el-icon-close"></i>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager"
        layout="total,sizes,prev,pager,next,jumper"
        :page-size="table.pageConfig.size ? table.pageConfig.size : 10"
        :page-sizes="[5, 10, 15, 20]"
        :total="table.pageConfig.total"
        :current-page.sync="table.pageConfig.current"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      ></el-pagination>
    </div>

    <!-- 新增请假对话框 -->
    <el-dialog
      title="提交请假申请"
      :visible.sync="dialogVisible"
      width="500px"
      @close="handleDialogClose"
    >
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="姓名">
          <el-input v-model="formData.name" disabled placeholder="自动获取"></el-input>
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="formData.phone" disabled placeholder="自动获取"></el-input>
        </el-form-item>
        <el-form-item label="请假类型" prop="typeNum">
          <el-select v-model="formData.typeNum" placeholder="请选择请假类型" style="width: 100%">
            <el-option label="事假" :value="0"></el-option>
            <el-option label="产假" :value="1"></el-option>
            <el-option label="病假" :value="2"></el-option>
            <el-option label="婚假" :value="3"></el-option>
            <el-option label="探亲假" :value="4"></el-option>
            <el-option label="陪产假" :value="5"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="请假天数" prop="days">
          <el-input-number v-model="formData.days" :min="1" :max="30" style="width: 100%"></el-input-number>
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker
            v-model="formData.startDate"
            type="date"
            placeholder="选择日期"
            style="width: 100%"
            value-format="yyyy-MM-dd"
            @change="handleStartDateChange"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker
            v-model="formData.endDate"
            type="date"
            placeholder="选择日期"
            style="width: 100%"
            value-format="yyyy-MM-dd"
            :picker-options="endDatePickerOptions"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入请假原因"
          ></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSubmit">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { add, edit, getListByStaffId } from '../../../api/staffLeave'
import { mapState } from 'vuex'

export default {
  name: 'MyLeave',
  data () {
    return {
      dialogVisible: false,
      formData: {
        name: '',
        phone: '',
        typeNum: null,
        days: 1,
        startDate: '',
        endDate: '',
        remark: ''
      },
      rules: {
        typeNum: [
          { required: true, message: '请选择请假类型', trigger: 'change' }
        ],
        days: [
          { required: true, message: '请输入请假天数', trigger: 'blur' }
        ],
        startDate: [
          { required: true, message: '请选择开始日期', trigger: 'change' }
        ],
        endDate: [
          { required: true, message: '请选择结束日期', trigger: 'change' }
        ]
      },
      table: {
        tableData: [],
        pageConfig: {
          total: 10,
          current: 1,
          size: 10
        }
      },
      endDatePickerOptions: {
        disabledDate: (time) => {
          if (this.formData.startDate) {
            return time.getTime() < new Date(this.formData.startDate).getTime()
          }
          return false
        }
      }
    }
  },
  computed: {
    ...mapState('staff', ['staff'])
  },
  methods: {
    // 获取请假类型名称
    getLeaveTypeName (typeNum) {
      // 后端已经序列化为中文,直接返回
      if (typeof typeNum === 'string') {
        return typeNum
      }
      // 如果是枚举对象,取message
      if (typeof typeNum === 'object' && typeNum !== null) {
        return typeNum.message || '未知'
      }
      // 如果是数字,转换为中文
      const typeMap = {
        0: '事假',
        1: '产假',
        2: '病假',
        3: '婚假',
        4: '探亲假',
        5: '陪产假'
      }
      return typeMap[typeNum] || '未知'
    },
    // 获取审核状态名称
    getStatusName (status) {
      // 后端已经序列化为中文,直接返回
      if (typeof status === 'string') {
        return status
      }
      // 如果是枚举对象,取message
      if (typeof status === 'object' && status !== null) {
        return status.message || '未知'
      }
      // 如果是数字,转换为中文
      const statusMap = {
        0: '未审核',
        1: '审核通过',
        2: '驳回',
        3: '撤销'
      }
      return statusMap[status] || '未知'
    },
    // 开始日期改变时，自动计算结束日期
    handleStartDateChange (value) {
      if (value && this.formData.days) {
        const startDate = new Date(value)
        const endDate = new Date(startDate.getTime() + (this.formData.days - 1) * 24 * 60 * 60 * 1000)
        const year = endDate.getFullYear()
        const month = String(endDate.getMonth() + 1).padStart(2, '0')
        const day = String(endDate.getDate()).padStart(2, '0')
        this.formData.endDate = `${year}-${month}-${day}`
      }
    },
    // 新增请假
    handleAdd () {
      // 自动填充姓名和电话
      this.formData.name = this.staff.name || ''
      this.formData.phone = this.staff.phone || ''
      this.dialogVisible = true
    },
    // 对话框关闭
    handleDialogClose () {
      this.$refs.formRef.resetFields()
      this.formData = {
        name: '',
        phone: '',
        typeNum: null,
        days: 1,
        startDate: '',
        endDate: '',
        remark: ''
      }
    },
    // 提交请假申请
    handleSubmit () {
      this.$refs.formRef.validate((valid) => {
        if (valid) {
          const data = {
            staffId: this.staff.id,
            typeNum: this.formData.typeNum,
            days: this.formData.days,
            startDate: this.formData.startDate,
            endDate: this.formData.endDate,
            remark: this.formData.remark
          }
          add(data).then(response => {
            if (response.code === 200) {
              this.$message.success('请假申请提交成功')
              this.dialogVisible = false
              this.loading()
            } else {
              this.$message.error(response.message)
            }
          })
        }
      })
    },
    // 撤销请假
    handleCancel (row) {
      this.$confirm('确定要撤销这条请假申请吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        row.staffLeave.status = row.cancel
        edit(row.staffLeave).then(response => {
          if (response.code === 200) {
            this.$message.success('撤销成功')
            this.loading()
          } else {
            this.$message.error(response.message)
          }
        })
      }).catch(() => {})
    },
    // 分页大小改变
    handleSizeChange (size) {
      this.table.pageConfig.size = size
      this.loading()
    },
    // 当前页改变
    handleCurrentChange (current) {
      this.table.pageConfig.current = current
      this.loading()
    },
    // 加载数据
    loading () {
      getListByStaffId({
        current: this.table.pageConfig.current,
        size: this.table.pageConfig.size,
        id: this.staff.id
      }).then(response => {
        if (response.code === 200) {
          this.table.tableData = response.data.list
          this.table.pageConfig.total = response.data.total
        } else {
          this.$message.error(response.message)
        }
      })
    }
  },
  created () {
    this.loading()
  }
}
</script>

<style lang="less" scoped>
.common-table {
  height: calc(100% - 62px);
  background-color: white;
  position: relative;

  .pager {
    position: absolute;
    bottom: 20px;
    right: 20px;
  }
}
</style>
