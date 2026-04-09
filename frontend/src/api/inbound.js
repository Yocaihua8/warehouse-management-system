import request from '../utils/request'

export const saveInboundOrder = (data) => {
    return request.post('/inbound-order/add', data)
}

export const updateInboundOrderDraft = (id, data) => {
    return request.put(`/inbound-order/${id}`, data)
}

export const confirmInboundOrder = (id) => {
    return request.post(`/inbound-order/${id}/confirm`)
}

export const voidInboundOrder = (id, voidReason) => {
    return request.post(`/inbound-order/${id}/void`, null, {
        params: {
            voidReason
        }
    })
}

export const getInboundOrderList = (params) => {
    return request.get('/inbound-order/list', { params })
}

export const getInboundOrderDetail = (id) => {
    return request.get(`/inbound-order/detail/${id}`)
}
