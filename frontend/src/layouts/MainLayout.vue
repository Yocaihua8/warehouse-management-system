<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="layout-aside">
      <div class="logo-area">仓库管理系统</div>

      <el-menu
          :default-active="activeMenu"
          :default-openeds="defaultOpeneds"
          class="menu"
          router
          unique-opened
      >
        <el-menu-item index="/">
          <span>首页</span>
        </el-menu-item>

        <el-menu-item index="/product/list">
          <span>商品管理</span>
        </el-menu-item>

        <el-menu-item index="/customer/list">
          <span>客户管理</span>
        </el-menu-item>

        <el-menu-item index="/stock/list">
          <span>库存管理</span>
        </el-menu-item>

        <el-sub-menu index="/inbound">
          <template #title>入库管理</template>
          <el-menu-item index="/inbound/list">入库单列表</el-menu-item>
          <el-menu-item index="/inbound/create">新增入库单</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/outbound">
          <template #title>出库管理</template>
          <el-menu-item index="/outbound/list">出库单列表</el-menu-item>
          <el-menu-item index="/outbound/create">新增出库单</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-title">{{ pageTitle }}</div>
      </el-header>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const activeMenu = computed(() => route.path)

const defaultOpeneds = computed(() => {
  const path = route.path

  if (path.startsWith('/inbound')) return ['/inbound']
  if (path.startsWith('/outbound')) return ['/outbound']

  return []
})

const pageTitle = computed(() => {
  const path = route.path

  if (path === '/') return '首页'
  if (path.startsWith('/product')) return '商品管理'
  if (path.startsWith('/customer')) return '客户管理'
  if (path.startsWith('/stock')) return '库存管理'
  if (path.startsWith('/inbound')) return '入库管理'
  if (path.startsWith('/outbound')) return '出库管理'

  return '仓库管理系统'
})
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
  background: #f5f7fa;
}

.layout-aside {
  background: #001529;
  color: #ffffff;
}

.logo-area {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.menu {
  border-right: none;
}

.layout-header {
  background: #ffffff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  padding: 0 20px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.layout-main {
  padding: 0;
  background: #f5f7fa;
}
</style>