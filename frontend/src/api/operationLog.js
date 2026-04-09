import request from '../utils/request'

export function getOperationLogList(params) {
    return request({
        url: '/operation/log/list',
        method: 'get',
        params
    })
}

