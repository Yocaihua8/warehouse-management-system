import axios from 'axios'

const request = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 10000
})

export const saveInboundOrder = (data) => {
    return request.post('/inbound-order/add', data)
}

export const getInboundOrderList = (params) => {
    return request.get('/inbound-order/list', { params })
}

export const getInboundOrderDetail = (id) => {
    return request.get(`/inbound-order/detail/${id}`)
}