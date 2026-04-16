// @vitest-environment jsdom

import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import ProductSelectDialog from '../ProductSelectDialog.vue'

const ElDialogStub = defineComponent({
  props: ['title', 'modelValue'],
  emits: ['close'],
  template: `
    <div v-if="modelValue" class="el-dialog-stub">
      <div class="dialog-title">{{ title }}</div>
      <slot />
      <slot name="footer" />
    </div>
  `
})

const ElInputStub = defineComponent({
  props: ['modelValue', 'placeholder', 'clearable'],
  emits: ['input'],
  template: `
    <div class="el-input-stub">
      <input
        :value="modelValue"
        :placeholder="placeholder"
        @input="$emit('input', $event.target.value)"
      />
      <slot name="append" />
    </div>
  `
})

const ElButtonStub = defineComponent({
  props: ['disabled', 'type', 'plain'],
  emits: ['click'],
  template: `<button :disabled="disabled" @click="$emit('click', $event)"><slot /></button>`
})

const ElTableStub = defineComponent({
  props: ['data'],
  emits: ['row-click', 'row-dblclick'],
  template: `<div class="el-table-stub"><slot /></div>`
})

const ElTableColumnStub = defineComponent({
  props: ['prop', 'label'],
  template: `<div class="el-table-column-stub">{{ label }}</div>`
})

const products = [
  { id: 1, productCode: 'P001', productName: '纯牛奶', specification: '1L', unit: '盒', salePrice: 12.5 },
  { id: 2, productCode: 'P002', productName: '酸奶', specification: '250ml', unit: '杯', salePrice: 6.8 }
]

const mountDialog = async (props = {}) => {
  const wrapper = mount(ProductSelectDialog, {
    attachTo: document.body,
    props: {
      visible: true,
      keyword: '牛奶',
      products,
      loading: false,
      selectedProductId: 1,
      ...props
    },
    global: {
      stubs: {
        teleport: true,
        transition: false,
        'el-dialog': ElDialogStub,
        'el-input': ElInputStub,
        'el-button': ElButtonStub,
        'el-table': ElTableStub,
        'el-table-column': ElTableColumnStub
      }
    }
  })

  await flushPromises()
  return wrapper
}

const findButtonByText = (wrapper, text) => {
  return wrapper.findAll('button').find(button => button.text().includes(text))
}

describe('ProductSelectDialog', () => {
  it('搜索输入和快速新建按钮应触发对应事件', async () => {
    const wrapper = await mountDialog()

    await wrapper.find('input').setValue('奶粉')
    await findButtonByText(wrapper, '快速新建商品').trigger('click')

    expect(wrapper.emitted('search')[0]).toEqual(['奶粉'])
    expect(wrapper.emitted('quick-create')[0]).toEqual(['牛奶'])
  })

  it('已选商品时应显示当前标签并确认回传当前商品', async () => {
    const wrapper = await mountDialog({ selectedProductId: 2 })

    expect(wrapper.text()).toContain('已选商品：P002 / 酸奶 / 250ml')

    await findButtonByText(wrapper, '确认').trigger('click')

    expect(wrapper.emitted('confirm')[0][0]).toMatchObject({
      id: 2,
      productCode: 'P002',
      productName: '酸奶'
    })
  })

  it('未选择商品时应显示未选择并允许取消关闭', async () => {
    const wrapper = await mountDialog({ selectedProductId: null })

    expect(wrapper.text()).toContain('已选商品：未选择')
    expect(findButtonByText(wrapper, '确认').element.disabled).toBe(true)

    await findButtonByText(wrapper, '取消').trigger('click')

    expect(wrapper.emitted('update:visible')[0]).toEqual([false])
  })
})
