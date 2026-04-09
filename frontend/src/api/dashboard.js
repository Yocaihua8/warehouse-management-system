import request from '../utils/request'

export function getDashboardData() {
    return request({
        url: '/dashboard',
        method: 'get'
    })
}

export function getDashboardTrend(days = 7) {
    return request({
        url: '/dashboard/trend',
        method: 'get',
        params: { days }
    })
}
