import request from '../utils/request'

const url = '/operlog'

/**
 * 分页条件查询操作日志
 * @param params
 * @returns {AxiosPromise}
 */
export const getList = (params) => {
  return request({
    url: url,
    method: 'get',
    params
  })
}

/**
 * 删除单条日志
 * @param id
 * @returns {AxiosPromise}
 */
export const deleteOne = (id) => {
  return request({
    url: url + '/' + id,
    method: 'delete'
  })
}

/**
 * 批量删除日志
 * @param ids
 * @returns {AxiosPromise}
 */
export const deleteBatch = (ids) => {
  return request({
    url: url + '/batch/' + ids,
    method: 'delete'
  })
}

/**
 * 清空日志
 * @returns {AxiosPromise}
 */
export const clear = () => {
  return request({
    url: url + '/clear',
    method: 'delete'
  })
}
