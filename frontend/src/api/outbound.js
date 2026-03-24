import axios from 'axios'

const request = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 5000
})

export const getOutboundList = (params) => {
    return request({
        url: '/outbound-order/list',
        method: 'get',
        params
    })
}

export const addOutboundOrder = (data) => {
    return request({
        url: '/outbound-order/add',
        method: 'post',
        data
    })
}

export const getOutboundDetail = (id) => {
    return request({
        url: `/outbound-order/${id}`,
        method: 'get'
    })
}