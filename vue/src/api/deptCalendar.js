import request from '@/utils/request'

// 批量保存部门上班日历
export function batchSaveDeptCalendar (data) {
  return request({
    url: '/deptCalendar/batchSave',
    method: 'post',
    data
  })
}

// 查询部门月份上班日历
export function getDeptMonthCalendar (params) {
  return request({
    url: '/deptCalendar/month',
    method: 'get',
    params
  })
}

// 查询某天是否上班
export function checkDeptWorkday (params) {
  return request({
    url: '/deptCalendar/checkWorkday',
    method: 'get',
    params
  })
}
