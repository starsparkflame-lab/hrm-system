<template>
  <div class="manage">
    <!------------- 搜索与操作 ---------------------->
    <div class="manage-header">
      <el-form label-width="auto" :model="searchForm.formData" :inline="true" size="mini">
        <el-form-item label="标题" prop="title">
          <el-input
            placeholder="请输入操作标题"
            v-model.trim="searchForm.formData.title"
            prefix-icon="el-icon-search"
          />
        </el-form-item>
        <el-form-item label="操作人" prop="operName">
          <el-input
            placeholder="请输入操作人"
            v-model.trim="searchForm.formData.operName"
            prefix-icon="el-icon-search"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="searchForm.formData.status" placeholder="全部" clearable style="width:100px">
            <el-option label="成功" :value="1"/>
            <el-option label="失败" :value="0"/>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search" size="mini">搜索 <i class="el-icon-search"/></el-button>
          <el-button type="danger" @click="reset" size="mini">重置 <i class="el-icon-refresh-left"/></el-button>
        </el-form-item>
      </el-form>
      <div style="margin-bottom: 10px">
        <el-popconfirm
          confirm-button-text='确定'
          cancel-button-text='我再想想'
          icon="el-icon-info"
          icon-color="red"
          title="你确定删除选中日志吗？"
          @confirm="handleDeleteBatch"
        >
          <el-button type="danger" size="mini" slot="reference">批量删除 <i class="el-icon-remove-outline"/></el-button>
        </el-popconfirm>
        <el-popconfirm
          style="margin-left: 10px"
          confirm-button-text='确定'
          cancel-button-text='我再想想'
          icon="el-icon-info"
          icon-color="red"
          title="清空后不可恢复，确定清空全部日志吗？"
          @confirm="handleClear"
        >
          <el-button type="warning" size="mini" slot="reference">清空 <i class="el-icon-delete"/></el-button>
        </el-popconfirm>
      </div>
    </div>

    <!------------------ 数据表格 ---------------->
    <div class="common-table">
      <el-table
        :data="table.tableData"
        height="85%"
        border
        stripe
        row-key="id"
        fit
        :header-cell-style="{background: '#eef1f6',color: '#606266',
        textAlign:'center',fontWeight:'bold',borderWidth:'3px'}"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center"/>
        <el-table-column type="expand">
          <template slot-scope="scope">
            <div style="padding: 8px 40px; word-break: break-all;">
              <p><b>请求参数：</b>{{ scope.row.operParam || '无' }}</p>
              <p><b>返回结果：</b>{{ scope.row.jsonResult || '无' }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="操作标题" min-width="140" align="center"/>
        <el-table-column prop="businessType" label="业务类型" min-width="90" align="center">
          <template slot-scope="scope">
            <el-tag :type="businessTagType(scope.row.businessType)" size="mini">
              {{ businessText(scope.row.businessType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestMethod" label="请求方式" min-width="90" align="center"/>
        <el-table-column prop="operUrl" label="请求地址" min-width="160" align="center" show-overflow-tooltip/>
        <el-table-column prop="operName" label="操作人" min-width="100" align="center"/>
        <el-table-column prop="operIp" label="操作IP" min-width="120" align="center"/>
        <el-table-column prop="status" label="状态" min-width="80" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="mini">
              {{ scope.row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" min-width="90" align="center"/>
        <el-table-column prop="operTime" label="操作时间" min-width="160" align="center"/>
        <el-table-column label="操作" width="110" fixed="right" align="center">
          <template slot-scope="scope">
            <el-popconfirm
              confirm-button-text='确定'
              cancel-button-text='我再想想'
              icon="el-icon-info"
              icon-color="red"
              title="你确定删除吗？"
              @confirm="handleDelete(scope.row.id)"
            >
              <el-button size="mini" type="danger" slot="reference">删除 <i class="el-icon-remove-outline"/></el-button>
            </el-popconfirm>
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
  </div>
</template>

<script>
import { clear, deleteBatch, deleteOne, getList } from '../../../api/operlog'

const BUSINESS_MAP = {
  0: '其他',
  1: '新增',
  2: '修改',
  3: '删除',
  4: '登录',
  5: '登出',
  6: '导出',
  7: '导入'
}

export default {
  name: 'Operlog',
  data () {
    return {
      searchForm: {
        formData: {}
      },
      table: {
        tableData: [],
        pageConfig: {
          total: 0,
          current: 1,
          size: 10
        }
      },
      ids: []
    }
  },
  methods: {
    businessText (type) {
      return BUSINESS_MAP[type] || '其他'
    },
    businessTagType (type) {
      switch (type) {
        case 1:
          return 'success'
        case 2:
          return 'warning'
        case 3:
          return 'danger'
        case 4:
        case 5:
          return 'info'
        default:
          return ''
      }
    },
    handleDelete (id) {
      deleteOne(id).then(response => {
        if (response.code === 200) {
          this.$message.success('删除成功！')
          this.loading()
        } else {
          this.$message.error('删除失败！')
        }
      })
    },
    handleDeleteBatch () {
      if (this.ids.length === 0) {
        this.$message.warning('请先勾选要删除的日志！')
        return
      }
      deleteBatch(this.ids).then(response => {
        if (response.code === 200) {
          this.$message.success('批量删除成功！')
          this.loading()
        } else {
          this.$message.error('批量删除失败！')
        }
      })
    },
    handleClear () {
      clear().then(response => {
        if (response.code === 200) {
          this.$message.success(response.message)
          this.loading()
        } else {
          this.$message.error('清空失败！')
        }
      })
    },
    search () {
      this.table.pageConfig.current = 1
      this.loading()
    },
    reset () {
      this.searchForm.formData = {}
      this.table.pageConfig.current = 1
      this.loading()
    },
    handleSizeChange (size) {
      this.table.pageConfig.size = size
      this.loading()
    },
    handleCurrentChange (current) {
      this.table.pageConfig.current = current
      this.loading()
    },
    handleSelectionChange (list) {
      this.ids = list.map(item => item.id)
    },
    loading () {
      getList({
        current: this.table.pageConfig.current,
        size: this.table.pageConfig.size,
        title: this.searchForm.formData.title,
        operName: this.searchForm.formData.operName,
        status: this.searchForm.formData.status
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
