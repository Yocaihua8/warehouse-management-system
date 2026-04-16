<template>
  <el-container class="layout-container">
    <el-aside :width="asideWidth" :class="['layout-aside', { 'is-collapsed': isSidebarCollapsed }]">
      <div class="logo-area" :title="isSidebarCollapsed ? '仓库管理系统' : ''">
        <el-icon class="logo-icon"><Box /></el-icon>
        <span v-if="!isSidebarCollapsed" class="logo-text">仓库管理系统</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :default-openeds="defaultOpeneds"
        :collapse="isSidebarCollapsed"
        class="menu"
        router
        unique-opened
      >
        <template v-for="entry in visibleMenuEntries" :key="entry.index">
          <el-menu-item
            v-if="!entry.children"
            :index="entry.index"
            :title="isSidebarCollapsed ? entry.title : ''"
          >
            <el-icon class="menu-icon"><component :is="entry.icon" /></el-icon>
            <span v-if="!isSidebarCollapsed">{{ entry.title }}</span>
          </el-menu-item>

          <el-sub-menu v-else :index="entry.index">
            <template #title>
              <span class="submenu-title" :title="isSidebarCollapsed ? entry.title : ''">
                <el-icon class="menu-icon"><component :is="entry.icon" /></el-icon>
                <span v-if="!isSidebarCollapsed">{{ entry.title }}</span>
              </span>
            </template>

            <el-menu-item v-for="child in entry.children" :key="child.index" :index="child.index">
              <el-icon class="menu-icon"><component :is="child.icon" /></el-icon>
              <span>{{ child.title }}</span>
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-button
            circle
            class="collapse-toggle"
            :title="isSidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
            @click="toggleSidebar"
          >
            <el-icon><component :is="isSidebarCollapsed ? Expand : Fold" /></el-icon>
          </el-button>
          <div class="header-title">{{ pageTitle }}</div>
        </div>

        <div class="header-actions">
          <span class="user-name">{{ displayName }}</span>
          <el-tag size="small" type="info">{{ roleText }}</el-tag>
          <el-button type="danger" link @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            <span>退出登录</span>
          </el-button>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, markRaw, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Avatar,
  Box,
  Coin,
  Document,
  Download,
  Expand,
  Fold,
  House,
  MagicStick,
  Memo,
  OfficeBuilding,
  SwitchButton,
  Tickets,
  Upload,
  User
} from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { logoutApi } from '../api/user'
import { clearAuth, getNickname, getRole, getUsername } from '../utils/auth'

const SIDEBAR_COLLAPSED_STORAGE_KEY = 'wms.layout.sidebar.collapsed'

const MENU_ENTRIES = [
  { index: '/', title: '首页', icon: markRaw(House) },
  { index: '/product/list', title: '商品管理', icon: markRaw(Box) },
  { index: '/customer/list', title: '客户管理', icon: markRaw(User) },
  { index: '/supplier/list', title: '供应商管理', icon: markRaw(OfficeBuilding) },
  { index: '/user/list', title: '用户管理', icon: markRaw(Avatar), adminOnly: true },
  { index: '/stock/list', title: '库存管理', icon: markRaw(Coin) },
  { index: '/stock/log/list', title: '库存流水', icon: markRaw(Tickets) },
  { index: '/operation/log/list', title: '操作日志', icon: markRaw(Document), adminOnly: true },
  {
    index: '/inbound',
    title: '入库管理',
    icon: markRaw(Download),
    children: [
      { index: '/inbound/list', title: '入库单列表', icon: markRaw(Document) },
      { index: '/inbound/create', title: '新增入库单', icon: markRaw(Memo) }
    ]
  },
  {
    index: '/outbound',
    title: '出库管理',
    icon: markRaw(Upload),
    children: [
      { index: '/outbound/list', title: '出库单列表', icon: markRaw(Document) },
      { index: '/outbound/create', title: '新增出库单', icon: markRaw(Memo) }
    ]
  },
  {
    index: '/ai',
    title: 'AI识别历史',
    icon: markRaw(MagicStick),
    children: [
      { index: '/ai/inbound/list', title: 'AI入库识别历史', icon: markRaw(Download) },
      { index: '/ai/outbound/list', title: 'AI出库识别历史', icon: markRaw(Upload) }
    ]
  }
]

const readSidebarCollapsed = () => {
  try {
    return window.localStorage.getItem(SIDEBAR_COLLAPSED_STORAGE_KEY) === '1'
  } catch (error) {
    return false
  }
}

const writeSidebarCollapsed = (collapsed) => {
  try {
    window.localStorage.setItem(SIDEBAR_COLLAPSED_STORAGE_KEY, collapsed ? '1' : '0')
  } catch (error) {
    // Ignore storage write failures and keep in-memory state.
  }
}

const route = useRoute()
const router = useRouter()
const isSidebarCollapsed = ref(readSidebarCollapsed())

const asideWidth = computed(() => {
  return isSidebarCollapsed.value ? '64px' : '220px'
})

const activeMenu = computed(() => route.path)

const defaultOpeneds = computed(() => {
  const path = route.path

  if (path.startsWith('/inbound')) return ['/inbound']
  if (path.startsWith('/outbound')) return ['/outbound']
  if (path.startsWith('/ai')) return ['/ai']

  return []
})

const pageTitle = computed(() => {
  const path = route.path

  if (path === '/') return '首页'
  if (path.startsWith('/product')) return '商品管理'
  if (path.startsWith('/customer')) return '客户管理'
  if (path.startsWith('/supplier')) return '供应商管理'
  if (path.startsWith('/user')) return '用户管理'
  if (path === '/stock/log/list') return '库存流水'
  if (path === '/operation/log/list') return '操作日志'
  if (path.startsWith('/stock')) return '库存管理'
  if (path.startsWith('/inbound')) return '入库管理'
  if (path.startsWith('/outbound')) return '出库管理'
  if (path.startsWith('/ai')) return 'AI识别管理'

  return '仓库管理系统'
})

const displayName = computed(() => {
  return getNickname() || getUsername() || '当前用户'
})

const roleText = computed(() => {
  const role = getRole()
  if (role === 'ADMIN') return '管理员'
  if (role === 'OPERATOR') return '操作员'
  return '未知角色'
})

const isAdmin = computed(() => getRole() === 'ADMIN')

const visibleMenuEntries = computed(() => {
  return MENU_ENTRIES.filter((entry) => !entry.adminOnly || isAdmin.value)
})

const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
  writeSidebarCollapsed(isSidebarCollapsed.value)
}

const handleLogout = async () => {
  try {
    await logoutApi()
  } catch (error) {
    console.error('退出登录失败：', error)
  } finally {
    clearAuth()
    ElMessage.success('已退出登录')
    await router.replace('/login')
  }
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
  background: #f5f7fa;
}

.layout-aside {
  background: #001529;
  color: #ffffff;
  overflow: hidden;
  transition: width 0.24s ease;
}

.logo-area {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  font-size: 18px;
  font-weight: 700;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  white-space: nowrap;
  transition: padding 0.24s ease;
}

.layout-aside.is-collapsed .logo-area {
  justify-content: center;
  padding: 0;
}

.logo-icon {
  flex-shrink: 0;
  font-size: 18px;
}

.logo-text {
  overflow: hidden;
  text-overflow: ellipsis;
}

.menu {
  border-right: none;
  min-height: calc(100vh - 60px);
  --el-menu-bg-color: #001529;
  --el-menu-text-color: rgba(255, 255, 255, 0.85);
  --el-menu-active-color: #ffffff;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.08);
}

.menu-icon {
  font-size: 18px;
}

.submenu-title {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.layout-header {
  background: #ffffff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.collapse-toggle {
  flex-shrink: 0;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-size: 14px;
  color: #606266;
}

.layout-main {
  padding: 0;
  background: #f5f7fa;
}

:deep(.menu .el-menu-item),
:deep(.menu .el-sub-menu__title) {
  display: flex;
  align-items: center;
  gap: 12px;
}

:deep(.menu.el-menu--collapse .el-menu-item),
:deep(.menu.el-menu--collapse .el-sub-menu__title) {
  gap: 0;
}
</style>
