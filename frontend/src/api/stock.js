import axios from 'axios'

const request = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 5000
})

export const getStockList = (params) => {
    return request({
        url: '/stock/list',
        method: 'get',
        params
    })
}