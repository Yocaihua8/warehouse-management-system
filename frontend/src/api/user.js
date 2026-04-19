import request from '../utils/request'

export function loginApi(data) {
    return request({
        url: '/user/login',
        method: 'post',
        data
    })
}

export function logoutApi() {
    return request({
        url: '/user/logout',
        method: 'post'
    })
}

export function getCurrentUserApi() {
    return request({
        url: '/user/me',
        method: 'post'
    })
}

export function getUserList(params) {
    return request({
        url: '/user/list',
        method: 'get',
        params
    })
}

export function addUser(data) {
    return request({
        url: '/user/add',
        method: 'post',
        data
    })
}

export function updateUser(data) {
    return request({
        url: '/user/update',
        method: 'put',
        data
    })
}

export function resetUserPassword(data) {
    return request({
        url: '/user/reset-password',
        method: 'put',
        data
    })
}

export function deleteUser(id) {
    return request({
        url: `/user/delete/${id}`,
        method: 'delete'
    })
}
