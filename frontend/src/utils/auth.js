import { pinia } from '../stores'
import { useAuthStore } from '../stores/auth'

function getAuthStore() {
    const store = useAuthStore(pinia)
    store.initFromStorage()
    return store
}

export function getToken() {
    return getAuthStore().token
}

export function setToken(token) {
    getAuthStore().setToken(token)
}

export function removeToken() {
    getAuthStore().setToken('')
}

export function getUsername() {
    return getAuthStore().username
}

export function setUsername(username) {
    getAuthStore().setUsername(username)
}

export function removeUsername() {
    getAuthStore().setUsername('')
}

export function getNickname() {
    return getAuthStore().nickname
}

export function setNickname(nickname) {
    getAuthStore().setNickname(nickname || '')
}

export function removeNickname() {
    getAuthStore().setNickname('')
}

export function getRole() {
    return getAuthStore().role
}

export function setRole(role) {
    getAuthStore().setRole(role || '')
}

export function removeRole() {
    getAuthStore().setRole('')
}

export function clearAuth() {
    getAuthStore().clearAuth()
}

export function buildLoginRedirectLocation(redirectPath = '') {
    const location = {
        path: '/login'
    }

    if (typeof redirectPath === 'string' && redirectPath.trim() && redirectPath !== '/login') {
        location.query = {
            redirect: redirectPath
        }
    }

    return location
}

export function clearAuthAndBuildLoginRedirect(redirectPath = '') {
    clearAuth()
    return buildLoginRedirectLocation(redirectPath)
}

export function redirectToLogin(router, redirectPath = '') {
    const location = clearAuthAndBuildLoginRedirect(redirectPath)
    if (router.currentRoute.value.path === '/login') {
        return Promise.resolve(location)
    }
    return router.replace(location)
}
