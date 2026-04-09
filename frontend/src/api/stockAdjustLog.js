import request from '../utils/request'

export function getStockAdjustLogList(params) {
    return request({
        url: '/stock/log/list',
        method: 'get',
        params
    })
}


