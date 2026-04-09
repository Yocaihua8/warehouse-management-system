import { defineStore } from 'pinia'

const TOKEN_KEY = 'token'
const USERNAME_KEY = 'username'
const NICKNAME_KEY = 'nickname'
const ROLE_KEY = 'role'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '',
    username: '',
    nickname: '',
    role: '',
    initialized: false
  }),
  getters: {
    isAdmin: (state) => state.role === 'ADMIN',
    isLoggedIn: (state) => !!state.token
  },
  actions: {
    initFromStorage() {
      if (this.initialized) {
        return
      }
      this.token = localStorage.getItem(TOKEN_KEY) || ''
      this.username = localStorage.getItem(USERNAME_KEY) || ''
      this.nickname = localStorage.getItem(NICKNAME_KEY) || ''
      this.role = localStorage.getItem(ROLE_KEY) || ''
      this.initialized = true
    },
    setToken(token) {
      const value = token || ''
      this.token = value
      if (value) {
        localStorage.setItem(TOKEN_KEY, value)
      } else {
        localStorage.removeItem(TOKEN_KEY)
      }
    },
    setUsername(username) {
      const value = username || ''
      this.username = value
      if (value) {
        localStorage.setItem(USERNAME_KEY, value)
      } else {
        localStorage.removeItem(USERNAME_KEY)
      }
    },
    setNickname(nickname) {
      const value = nickname || ''
      this.nickname = value
      if (value) {
        localStorage.setItem(NICKNAME_KEY, value)
      } else {
        localStorage.removeItem(NICKNAME_KEY)
      }
    },
    setRole(role) {
      const value = role || ''
      this.role = value
      if (value) {
        localStorage.setItem(ROLE_KEY, value)
      } else {
        localStorage.removeItem(ROLE_KEY)
      }
    },
    clearAuth() {
      this.token = ''
      this.username = ''
      this.nickname = ''
      this.role = ''
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USERNAME_KEY)
      localStorage.removeItem(NICKNAME_KEY)
      localStorage.removeItem(ROLE_KEY)
    }
  }
})
