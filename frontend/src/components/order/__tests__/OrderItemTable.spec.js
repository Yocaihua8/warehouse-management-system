// @vitest-environment jsdom

import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import OrderItemTable from '../OrderItemTable.vue'

const ResizeObserverMock = class {
  observe() {}
  unobserve() {}
  disconnect() {}
}

beforeAll(() => {
  global.ResizeObserver = ResizeObserverMock
  window.ResizeObserver = ResizeObserverMock
  window.matchMedia = window.matchMedia || (() => ({
    matches: false,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false
  }))
  Element.prototype.scrollIntoView = Element.prototype.scrollIntoView || (() => {})
})

const createItem = (overrides = {}) => ({
  productId: 10,
  productCode: 'P010',
  productName: '商品A',
  specification: '500g',
  unit: '袋',
  availableStock: 8,
  quantity: 2,
  unitPrice: 3.5,
  remark: '测试备注',
  ...overrides
})

const mountTable = async (props = {}) => {
  const wrapper = mount(OrderItemTable, {
    attachTo: document.body,
    props: {
      title: '出库明细',
      items: [createItem()],
      calcAmount: (row) => (Number(row?.quantity || 0) * Number(row?.unitPrice || 0)).toFixed(2),
      ...props
    },
    global: {
      plugins: [ElementPlus],
      stubs: {
        teleport: true,
        transition: false
      }
    }
  })

  await flushPromises()
  return wrapper
}

const findButtonByText = (wrapper, text) => {
  return wrapper.findAll('button').find(button => button.text().includes(text))
}

describe('OrderItemTable', () => {
  it('新增明细按钮应同时触发 row-add 和 add-item', async () => {
    const wrapper = await mountTable()

    await findButtonByText(wrapper, '新增明细').trigger('click')

    expect(wrapper.emitted('row-add')).toHaveLength(1)
    expect(wrapper.emitted('add-item')).toHaveLength(1)
  })

  it('商品列点击选择和清空应触发对应事件', async () => {
    const wrapper = await mountTable()

    await findButtonByText(wrapper, '选择').trigger('click')
    await findButtonByText(wrapper, '清空').trigger('click')

    expect(wrapper.emitted('open-product-dialog')).toEqual([[0]])
    expect(wrapper.emitted('product-selected')[0][0]).toMatchObject({
      index: 0,
      productId: null
    })
    expect(wrapper.emitted('product-change')[0][0]).toMatchObject({
      index: 0,
      productId: null
    })
  })

  it('出库模式应显示库存列并渲染合计行', async () => {
    const wrapper = await mountTable({
      orderType: 'outbound',
      items: [
        createItem({ quantity: 2, unitPrice: 3.5 }),
        createItem({ productId: 11, productCode: 'P011', productName: '商品B', quantity: 1, unitPrice: 4 })
      ]
    })

    expect(wrapper.text()).toContain('库存余量')
    expect(wrapper.text()).toContain('合计')
    expect(wrapper.text()).toContain('11.00')
  })

  it('只读模式应显示文本且不渲染商品操作按钮', async () => {
    const wrapper = await mountTable({
      editable: false,
      showAddButton: false,
      showActionColumn: false,
      items: [
        createItem({
          productId: null,
          productCode: '',
          productName: '',
          specification: '',
          unit: '',
          availableStock: null
        })
      ]
    })

    expect(wrapper.text()).toContain('未选择商品')
    expect(wrapper.text()).toContain('-')
    expect(findButtonByText(wrapper, '选择')).toBeUndefined()
    expect(findButtonByText(wrapper, '清空')).toBeUndefined()
  })
})
