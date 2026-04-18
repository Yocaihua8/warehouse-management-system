// @vitest-environment jsdom

import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  route: {
    path: '/inbound/create',
    query: {}
  },
  routerPush: vi.fn(),
  routerReplace: vi.fn(),
  onBeforeRouteLeave: vi.fn(),
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
  messageWarning: vi.fn(),
  messageConfirm: vi.fn(),
  getProductList: vi.fn(),
  getInboundOrderDetail: vi.fn(),
  saveInboundOrder: vi.fn(),
  updateInboundOrderDraft: vi.fn(),
  confirmInboundOrder: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  useRouter: () => ({
    push: mocks.routerPush,
    replace: mocks.routerReplace
  }),
  onBeforeRouteLeave: mocks.onBeforeRouteLeave
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: mocks.messageSuccess,
    error: mocks.messageError,
    warning: mocks.messageWarning
  },
  ElMessageBox: {
    confirm: mocks.messageConfirm
  }
}))

vi.mock('../../api/product', () => ({
  getProductList: mocks.getProductList
}))

vi.mock('../../api/inbound', () => ({
  getInboundOrderDetail: mocks.getInboundOrderDetail,
  saveInboundOrder: mocks.saveInboundOrder,
  updateInboundOrderDraft: mocks.updateInboundOrderDraft,
  confirmInboundOrder: mocks.confirmInboundOrder
}))

import { useInboundCreatePage } from '../useInboundCreatePage'

const wrappers = []

const mountComposable = async () => {
  let api
  const Harness = defineComponent({
    setup() {
      api = useInboundCreatePage()
      return () => null
    }
  })

  const wrapper = mount(Harness)
  wrappers.push(wrapper)
  await flushPromises()
  return api
}

describe('useInboundCreatePage', () => {
  beforeEach(() => {
    mocks.route.path = '/inbound/create'
    mocks.route.query = {}
    mocks.routerPush.mockReset()
    mocks.routerReplace.mockReset()
    mocks.onBeforeRouteLeave.mockReset()
    mocks.messageSuccess.mockReset()
    mocks.messageError.mockReset()
    mocks.messageWarning.mockReset()
    mocks.messageConfirm.mockReset()
    mocks.getProductList.mockReset()
    mocks.getInboundOrderDetail.mockReset()
    mocks.saveInboundOrder.mockReset()
    mocks.updateInboundOrderDraft.mockReset()
    mocks.confirmInboundOrder.mockReset()

    mocks.getProductList.mockResolvedValue({
      data: {
        code: 1,
        data: {
          list: [
            {
              id: 10,
              productCode: 'P001',
              productName: '纯牛奶',
              specification: '1L',
              unit: '盒'
            }
          ],
          total: 1
        }
      }
    })
  })

  afterEach(() => {
    while (wrappers.length > 0) {
      wrappers.pop().unmount()
    }
  })

  it('保存草稿 happy path 应调用保存接口并跳转到入库列表', async () => {
    const api = await mountComposable()

    api.form.supplierName = '  测试供应商  '
    api.form.itemList.splice(1)
    Object.assign(api.form.itemList[0], {
      productId: 10,
      quantity: 2,
      unitPrice: 6.5,
      remark: '首行备注'
    })

    mocks.saveInboundOrder.mockResolvedValue({
      data: {
        code: 1,
        data: {
          id: 101,
          orderNo: 'RK20260418001'
        }
      }
    })

    const draftId = await api.handleSubmit()

    expect(mocks.saveInboundOrder).toHaveBeenCalledWith({
      supplierName: '测试供应商',
      remark: '',
      itemList: [
        {
          productId: 10,
          quantity: 2,
          unitPrice: 6.5,
          remark: '首行备注'
        }
      ]
    })
    expect(mocks.messageSuccess).toHaveBeenCalledWith('保存草稿成功，单号：RK20260418001')
    expect(mocks.routerPush).toHaveBeenCalledWith('/inbound/list')
    expect(draftId).toBe(101)
  })

  it('保存草稿 error path 应提示错误且不跳转', async () => {
    const api = await mountComposable()

    api.form.supplierName = '测试供应商'
    api.form.itemList.splice(1)
    Object.assign(api.form.itemList[0], {
      productId: 10,
      quantity: 1,
      unitPrice: 3
    })

    mocks.saveInboundOrder.mockResolvedValue({
      data: {
        code: 0,
        message: '保存入库草稿失败'
      }
    })

    const draftId = await api.handleSubmit()

    expect(mocks.saveInboundOrder).toHaveBeenCalledTimes(1)
    expect(mocks.messageError).toHaveBeenCalledWith('保存入库草稿失败')
    expect(mocks.routerPush).not.toHaveBeenCalled()
    expect(draftId).toBeNull()
  })
})
