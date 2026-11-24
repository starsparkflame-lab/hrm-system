import request from '@/utils/request'

// 员工签到
export function doCheckin (data) {
  return request({
    url: '/checkin/do',
    method: 'post',
    data: data
  })
}

// 查询今日签到记录
export function getTodayCheckins (staffId) {
  return request({
    url: `/checkin/today/${staffId}`,
    method: 'get'
  })
}

// 查询签到状态
export function getCheckinStatus (staffId) {
  return request({
    url: `/checkin/status/${staffId}`,
    method: 'get'
  })
}

// 获取部门作息时间
export function getDeptWorkTime (staffId) {
  return request({
    url: `/checkin/dept-time/${staffId}`,
    method: 'get'
  })
}
