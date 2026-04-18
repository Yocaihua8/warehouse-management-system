// @vitest-environment jsdom

import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  route: {
    path: '/outbound/create',
    query: {}
  },
  routerPush: vi.fn(),
  routerReplace: vi.fn(),
  onBeforeRouteLeave: vi.fn(),
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
  messageWarning: vi.fn(),
  messageConfirm: vi.fn(),
  getCustomerList: vi.fn(),
  getProductList: vi.fn(),
  getStockList: vi.fn(),
  addOutboundOrder: vi.fn(),
  getOutboundDetail: vi.fn(),
  updateOutboundOrderDraft: vi.fn(),
  confirmOutboundOrder: vi.fn()
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

vi.mock('../../api/customer', () => ({
  getCustomerList: mocks.getCustomerList
}))

vi.mock('../../api/product', () => ({
  getProductList: mocks.getProductList
}))

vi.mock('../../api/stock', () => ({
  getStockList: mocks.getStockList
}))

vi.mock('../../api/outbound', () => ({
  addOutboundOrder: mocks.addOutboundOrder,
  getOutboundDetail: mocks.getOutboundDetail,
  updateOutboundOrderDraft: mocks.updateOutboundOrderDraft,
  confirmOutboundOrder: mocks.confirmOutboundOrder
}))

import { useOutboundCreatePage } from '../useOutboundCreatePage'

const wrappers = []

const mountComposable = async () => {
  let api
  const Harness = defineComponent({
    setup() {
      api = useOutboundCreatePage()
      return () => null
    }
  })

  const wrapper = mount(Harness)
  wrappers.push(wrapper)
  await flushPromises()
  return api
}

describe('useOutboundCreatePage', () => {
  beforeEach(() => {
    mocks.route.path = '/outbound/create'
    mocks.route.query = {}
    mocks.routerPush.mockReset()
    mocks.routerReplace.mockReset()
    mocks.onBeforeRouteLeave.mockReset()
    mocks.messageSuccess.mockReset()
    mocks.messageError.mockReset()
    mocks.messageWarning.mockReset()
    mocks.messageConfirm.mockReset()
    mocks.getCustomerList.mockReset()
    mocks.getProductList.mockReset()
    mocks.getStockList.mockReset()
    mocks.addOutboundOrder.mockReset()
    mocks.getOutboundDetail.mockReset()
    mocks.updateOutboundOrderDraft.mockReset()
    mocks.confirmOutboundOrder.mockReset()

    mocks.getCustomerList.mockResolvedValue({
      data: {
        code: 1,
        data: {
          list: [{ id: 9, customerCode: 'C001', customerName: '测试客户' }],
          total: 1
        }
      }
    })
    mocks.getProductList.mockResolvedValue({
      data: {
        code: 1,
        data: {
          list: [{ id: 10, productCode: 'P001', productName: '纯牛奶' }],
          total: 1
        }
      }
    })
    mocks.getStockList.mockResolvedValue({
      data: {
        code: 1,
        data: {
          list: [{ productId: 10, quantity: 99 }],
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

  it('保存出库草稿 happy path 应调用保存接口并跳转到出库列表', async () => {
    const api = await mountComposable()

    api.form.customerId = 9
    api.form.itemList.splice(1)
    Object.assign(api.form.itemList[0], {
      productId: 10,
      quantity: 3,
      unitPrice: 8.8,
      remark: '出库备注'
    })

    mocks.addOutboundOrder.mockResolvedValue({
      data: {
        code: 1,
        data: {
          id: 202,
          orderNo: 'CK20260418001'
        }
      }
    })

    const draftId = await api.handleSubmit()

    expect(mocks.addOutboundOrder).toHaveBeenCalledWith({
      customerId: 9,
      remark: '',
      itemList: [
        {
          productId: 10,
          quantity: 3,
          unitPrice: 8.8,
          remark: '出库备注'
        }
      ]
    })
    expect(mocks.messageSuccess).toHaveBeenCalledWith('保存出库单草稿成功，单号：CK20260418001')
    expect(mocks.routerPush).toHaveBeenCalledWith('/outbound/list')
    expect(draftId).toBe(202)
  })

  it('保存出库草稿 error path 应提示错误且不跳转', async () => {
    const api = await mountComposable()

    api.form.customerId = 9
    api.form.itemList.splice(1)
    Object.assign(api.form.itemList[0], {
      productId: 10,
      quantity: 2,
      unitPrice: 5.5
    })

    mocks.addOutboundOrder.mockResolvedValue({
      data: {
        code: 0,
        message: '保存出库草稿失败'
      }
    })

    const draftId = await api.handleSubmit()

    expect(mocks.addOutboundOrder).toHaveBeenCalledTimes(1)
    expect(mocks.messageError).toHaveBeenCalledWith('保存出库草稿失败')
    expect(mocks.routerPush).not.toHaveBeenCalled()
    expect(draftId).toBeNull()
  })
})
