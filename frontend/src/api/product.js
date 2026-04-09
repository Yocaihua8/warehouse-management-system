import request from '../utils/request'

export const getProductList = (params) => {
    return request({
        url: '/product/list',
        method: 'get',
        params
    })
}

export const addProduct = (data) => {
    return request({
        url: '/product/add',
        method: 'post',
        data
    })
}

export const getProductDetail = (id) => {
    return request({
        url: `/product/${id}`,
        method: 'get'
    })
}

export const updateProduct = (data) => {
    return request({
        url: '/product/update',
        method: 'put',
        data
    })
}

export const deleteProduct = (id) => {
    return request({
        url: `/product/delete/${id}`,
        method: 'delete'
    })
}