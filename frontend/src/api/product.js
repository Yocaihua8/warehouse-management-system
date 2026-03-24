import axios from 'axios'

const request = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 5000
})

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
        url: `/product/detail/${id}`,
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