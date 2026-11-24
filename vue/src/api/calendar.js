import request from '@/utils/request'

export function getDeptCalendar (params) {
  return request({
    url: '/calendar/dept',
    method: 'get',
    params
  })
}
