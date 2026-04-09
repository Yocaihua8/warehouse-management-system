import request from '../utils/request'

export const getCustomerList = (params) => {
    return request({
        url: '/customer/list',
        method: 'get',
        params
    })
}

export const addCustomer = (data) => {
    return request({
        url: '/customer/add',
        method: 'post',
        data
    })
}

export const getCustomerDetail = (id) => {
    return request({
        url: `/customer/${id}`,
        method: 'get'
    })
}

export const updateCustomer = (data) => {
    return request({
        url: '/customer/update',
        method: 'put',
        data
    })
}

export const deleteCustomer = (id) => {
    return request({
        url: `/customer/delete/${id}`,
        method: 'delete'
    })
}
