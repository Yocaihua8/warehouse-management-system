import request from '../utils/request'

export function recognizeInbound(formData) {
    return request({
        url: '/ai/inbound/recognize',
        method: 'post',
        data: formData,
        timeout: 120000
    })
}

export function recognizeOutbound(formData) {
    return request({
        url: '/ai/outbound/recognize',
        method: 'post',
        data: formData,
        timeout: 120000
    })
}

export const confirmInbound = (data) => {
    return request({
        url: '/ai/inbound/confirm',
        method: 'post',
        data
    })
}

export const confirmOutbound = (data) => {
    return request({
        url: '/ai/outbound/confirm',
        method: 'post',
        data
    })
}

export const getAiInboundRecordList = (params) => {
    return request({
        url: '/ai/inbound/list',
        method: 'get',
        params
    })
}

export const getAiInboundRecordDetail = (id) => {
    return request({
        url: `/ai/inbound/detail/${id}`,
        method: 'get'
    })
}

export const getAiOutboundRecordList = (params) => {
    return request({
        url: '/ai/outbound/list',
        method: 'get',
        params
    })
}

export const getAiOutboundRecordDetail = (id) => {
    return request({
        url: `/ai/outbound/detail/${id}`,
        method: 'get'
    })
}
