import request from '../utils/request'

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

export const updateOutboundOrderDraft = (id, data) => {
    return request({
        url: `/outbound-order/${id}`,
        method: 'put',
        data
    })
}

export const confirmOutboundOrder = (id) => {
    return request({
        url: `/outbound-order/${id}/confirm`,
        method: 'post'
    })
}

export const voidOutboundOrder = (id, voidReason) => {
    return request({
        url: `/outbound-order/${id}/void`,
        method: 'post',
        params: {
            voidReason
        }
    })
}

export const getOutboundDetail = (id) => {
    return request({
        url: `/outbound-order/${id}`,
        method: 'get'
    })
}
