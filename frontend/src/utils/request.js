import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { getToken, redirectToLogin } from './auth'

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').trim()

const request = axios.create({
    baseURL: apiBaseUrl,
    timeout: 20000
})

let handlingUnauthorized = false

request.interceptors.request.use(
    config => {
        const token = getToken()
        if (token) {
            config.headers.token = token
        }
        return config
    },
    error => Promise.reject(error)
)

request.interceptors.response.use(
    response => response,
    error => {
        if (error.response && error.response.status === 401) {
            if (!handlingUnauthorized) {
                handlingUnauthorized = true
                ElMessage.error('未登录或登录已失效，请重新登录')
                redirectToLogin(router, router.currentRoute.value.fullPath)
                setTimeout(() => {
                    handlingUnauthorized = false
                }, 300)
            }
        }
        return Promise.reject(error)
    }
)

export default request
