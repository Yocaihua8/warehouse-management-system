<template>
  <div class="login-page">
    <el-card class="login-card" shadow="hover">
      <template #header>
        <div class="login-title">仓库管理系统登录</div>
      </template>

      <el-form :model="loginForm" @keyup.enter="handleLogin">
        <el-form-item label="用户名">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>

        <el-form-item label="密码">
          <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              show-password
              clearable
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="handleLogin">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { loginApi } from '../api/user'
import { setNickname, setRole, setToken, setUsername } from '../utils/auth'

const router = useRouter()
const route = useRoute()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const handleLogin = async () => {
  if (!loginForm.username) {
    ElMessage.warning('请输入用户名')
    return
  }

  if (!loginForm.password) {
    ElMessage.warning('请输入密码')
    return
  }

  loading.value = true
  try {
    const res = await loginApi(loginForm)
    const result = res.data

    if (result.code === 1) {
      setToken(result.data.token)
      setUsername(result.data.username)
      setNickname(result.data.nickname)
      setRole(result.data.role)
      ElMessage.success('登录成功')
      const redirectPath = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
      router.push(redirectPath)
    } else {
      ElMessage.error(result.message || '登录失败')
    }
  } catch (error) {
    ElMessage.error('请求登录接口失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f5f7fa;
}

.login-card {
  width: 420px;
}

.login-title {
  text-align: center;
  font-size: 20px;
  font-weight: 600;
}
</style>
