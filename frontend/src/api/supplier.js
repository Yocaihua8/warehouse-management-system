import request from '../utils/request'

export const getSupplierList = (params) => {
    return request({
        url: '/supplier/list',
        method: 'get',
        params
    })
}

export const addSupplier = (data) => {
    return request({
        url: '/supplier/add',
        method: 'post',
        data
    })
}
