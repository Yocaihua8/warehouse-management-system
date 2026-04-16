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

export const getSupplierDetail = (id) => {
    return request({
        url: `/supplier/${id}`,
        method: 'get'
    })
}

export const updateSupplier = (data) => {
    return request({
        url: '/supplier/update',
        method: 'put',
        data
    })
}

export const deleteSupplier = (id) => {
    return request({
        url: `/supplier/delete/${id}`,
        method: 'delete'
    })
}
