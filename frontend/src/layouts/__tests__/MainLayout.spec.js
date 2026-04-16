// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const SIDEBAR_COLLAPSED_STORAGE_KEY = 'wms.layout.sidebar.collapsed'

const mocks = vi.hoisted(() => ({
  route: { path: '/' },
  routerReplace: vi.fn(),
  logoutApi: vi.fn(),
  clearAuth: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  useRouter: () => ({
    replace: mocks.routerReplace
  })
}))

vi.mock('../../api/user', () => ({
  logoutApi: mocks.logoutApi
}))

vi.mock('../../utils/auth', () => ({
  clearAuth: mocks.clearAuth,
  getNickname: () => '测试用户',
  getRole: () => 'ADMIN',
  getUsername: () => 'tester'
}))

import MainLayout from '../MainLayout.vue'

const ElContainerStub = defineComponent({
  template: '<div class="el-container-stub"><slot /></div>'
})

const ElAsideStub = defineComponent({
  props: ['width'],
  template: '<aside class="el-aside-stub" :data-width="width"><slot /></aside>'
})

const ElHeaderStub = defineComponent({
  template: '<header class="el-header-stub"><slot /></header>'
})

const ElMainStub = defineComponent({
  template: '<main class="el-main-stub"><slot /></main>'
})

const ElMenuStub = defineComponent({
  props: ['collapse', 'defaultActive', 'defaultOpeneds', 'router', 'uniqueOpened'],
  template: '<nav class="el-menu-stub" :data-collapse="collapse"><slot /></nav>'
})

const ElMenuItemStub = defineComponent({
  props: ['index'],
  template: '<div class="el-menu-item-stub" :data-index="index"><slot /></div>'
})

const ElSubMenuStub = defineComponent({
  props: ['index'],
  template: `
    <div class="el-sub-menu-stub" :data-index="index">
      <div class="el-sub-menu-title"><slot name="title" /></div>
      <div class="el-sub-menu-content"><slot /></div>
    </div>
  `
})

const ElButtonStub = defineComponent({
  emits: ['click'],
  template: '<button v-bind="$attrs" @click="$emit(\'click\', $event)"><slot /></button>'
})

const ElTagStub = defineComponent({
  template: '<span class="el-tag-stub"><slot /></span>'
})

const ElIconStub = defineComponent({
  template: '<span class="el-icon-stub"><slot /></span>'
})

const mountLayout = () => {
  return mount(MainLayout, {
    global: {
      stubs: {
        'router-view': true,
        'el-container': ElContainerStub,
        'el-aside': ElAsideStub,
        'el-header': ElHeaderStub,
        'el-main': ElMainStub,
        'el-menu': ElMenuStub,
        'el-menu-item': ElMenuItemStub,
        'el-sub-menu': ElSubMenuStub,
        'el-button': ElButtonStub,
        'el-tag': ElTagStub,
        'el-icon': ElIconStub,
        transition: false,
        teleport: true
      }
    }
  })
}

describe('MainLayout', () => {
  beforeEach(() => {
    mocks.route.path = '/'
    localStorage.clear()
    mocks.routerReplace.mockReset()
    mocks.logoutApi.mockReset()
    mocks.clearAuth.mockReset()
  })

  it('Header 左侧应显示折叠按钮并在点击后切换侧栏宽度', async () => {
    const wrapper = mountLayout()

    expect(wrapper.find('.layout-aside').classes()).not.toContain('is-collapsed')
    expect(wrapper.find('.el-aside-stub').attributes('data-width')).toBe('220px')
    expect(wrapper.find('button[title="收起侧边栏"]').exists()).toBe(true)
    expect(wrapper.find('.logo-text').text()).toBe('仓库管理系统')

    await wrapper.find('button[title="收起侧边栏"]').trigger('click')

    expect(wrapper.find('.layout-aside').classes()).toContain('is-collapsed')
    expect(wrapper.find('.el-aside-stub').attributes('data-width')).toBe('64px')
    expect(wrapper.find('button[title="展开侧边栏"]').exists()).toBe(true)
    expect(localStorage.getItem(SIDEBAR_COLLAPSED_STORAGE_KEY)).toBe('1')
  })

  it('刷新后应从 localStorage 恢复折叠状态，并为折叠菜单项提供标题提示', () => {
    localStorage.setItem(SIDEBAR_COLLAPSED_STORAGE_KEY, '1')
    mocks.route.path = '/inbound/list'

    const wrapper = mountLayout()

    expect(wrapper.find('.layout-aside').classes()).toContain('is-collapsed')
    expect(wrapper.find('.el-aside-stub').attributes('data-width')).toBe('64px')
    expect(wrapper.find('.logo-text').exists()).toBe(false)
    expect(wrapper.find('[data-index="/"]').attributes('title')).toBe('首页')
    expect(wrapper.find('.submenu-title[title="入库管理"]').exists()).toBe(true)
  })

  it('管理员菜单项应显示图标和对应入口', () => {
    mocks.route.path = '/user/list'

    const wrapper = mountLayout()

    expect(wrapper.text()).toContain('用户管理')
    expect(wrapper.text()).toContain('操作日志')
    expect(wrapper.findAll('.menu-icon').length).toBeGreaterThanOrEqual(10)
  })
})
