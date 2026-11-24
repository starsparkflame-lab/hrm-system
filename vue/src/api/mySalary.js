import request from '@/utils/request'

// 查询员工个人工资
export function listMySalary (query) {
  return request({
    url: '/salary/my',
    method: 'get',
    params: query
  })
}
