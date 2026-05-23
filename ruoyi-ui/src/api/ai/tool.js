import request from '@/utils/request'

// 查询工具列表
export function listTool(query) {
  return request({
    url: '/ai/tool/list',
    method: 'get',
    params: query
  })
}

// 查询工具详细
export function getTool(toolId) {
  return request({
    url: '/ai/tool/' + toolId,
    method: 'get'
  })
}

// 新增工具
export function addTool(data) {
  return request({
    url: '/ai/tool',
    method: 'post',
    data: data
  })
}

// 修改工具
export function updateTool(data) {
  return request({
    url: '/ai/tool',
    method: 'put',
    data: data
  })
}

// 删除工具
export function delTool(toolIds) {
  return request({
    url: '/ai/tool/' + toolIds,
    method: 'delete'
  })
}
