import request from '../utils/request'

export function getStockList(params) {
    return request({
        url: '/stock/list',
        method: 'get',
        params
    })
}

export function exportStockList(params) {
    return request({
        url: '/stock/export',
        method: 'get',
        params,
        responseType: 'blob'
    })
}

export function updateStock(data) {
    return request({
        url: '/stock/update',
        method: 'put',
        data
    })
}
