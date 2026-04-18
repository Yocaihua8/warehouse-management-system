// @vitest-environment jsdom

import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  routerPush: vi.fn(),
  handleSubmit: vi.fn(),
  handleSubmitConfirm: vi.fn(),
  handleClear: vi.fn(),
  openAiDialog: vi.fn(),
  openPrintPreview: vi.fn(),
  handleSaveAndNew: vi.fn(),
  focusFirstEditableCell: vi.fn()
}))

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return {
    ...actual,
    useRouter: () => ({
      push: mocks.routerPush
    })
  }
})

vi.mock('../../../utils/auth', () => ({
  getNickname: () => '测试昵称',
  getUsername: () => 'tester'
}))

vi.mock('../../../composables/useInboundCreatePage', () => ({
  useInboundCreatePage: () => ({
    aiDialogRef: { value: null },
    form: { itemList: [{ productId: 1 }], remark: '', orderNo: '' },
    editable: true,
    pageTitle: '新增入库单',
    submitting: false,
    currentDraftId: null,
    productOptions: [],
    productLoading: false,
    summary: { totalQuantity: 0, totalAmount: 0 },
    calcAmount: vi.fn(() => 0),
    addItem: vi.fn(),
    insertItem: vi.fn(),
    removeItem: vi.fn(),
    updateRowField: vi.fn(),
    handleProductSearch: vi.fn(),
    onProductChange: vi.fn(),
    openAiDialog: mocks.openAiDialog,
    handleClear: mocks.handleClear,
    handleSubmit: mocks.handleSubmit,
    handleSaveAndNew: mocks.handleSaveAndNew,
    handleSubmitConfirm: mocks.handleSubmitConfirm,
    openPrintPreview: mocks.openPrintPreview,
    canSaveDraft: true,
    canSaveAndNew: true,
    canClear: true,
    canAiImport: true,
    canSubmitConfirm: true,
    canPrintPreview: false
  })
}))

import InboundOrderCreate from '../InboundOrderCreate.vue'

const OrderHeaderFormStub = defineComponent({
  name: 'OrderHeaderForm',
  template: '<div class="order-header-form-stub"></div>'
})

const OrderDetailTableStub = defineComponent({
  name: 'OrderDetailTable',
  setup(_, { expose }) {
    expose({
      focusFirstEditableCell: mocks.focusFirstEditableCell
    })
    return () => h('div', { class: 'order-detail-table-stub' })
  }
})

const OrderSummaryBarStub = defineComponent({
  name: 'OrderSummaryBar',
  emits: ['save-draft', 'submit-order', 'cancel'],
  template: `
    <div class="order-summary-bar-stub">
      <slot name="aux-actions" />
      <button class="save-draft-trigger" @click="$emit('save-draft')">触发保存草稿</button>
      <button class="submit-trigger" @click="$emit('submit-order')">触发提交确认</button>
      <button class="cancel-trigger" @click="$emit('cancel')">触发取消</button>
    </div>
  `
})

const AiRecognitionDialogStub = defineComponent({
  name: 'AiRecognitionDialog',
  template: '<div class="ai-recognition-dialog-stub"></div>'
})

const ElButtonStub = defineComponent({
  props: ['disabled', 'type', 'plain', 'loading'],
  emits: ['click'],
  template: '<button :disabled="disabled" @click="$emit(\'click\', $event)"><slot /></button>'
})

const mountView = () => {
  return mount(InboundOrderCreate, {
    global: {
      stubs: {
        'el-button': ElButtonStub,
        OrderHeaderForm: OrderHeaderFormStub,
        OrderDetailTable: OrderDetailTableStub,
        OrderSummaryBar: OrderSummaryBarStub,
        AiRecognitionDialog: AiRecognitionDialogStub
      }
    }
  })
}

describe('InboundOrderCreate', () => {
  beforeEach(() => {
    mocks.routerPush.mockReset()
    mocks.handleSubmit.mockReset()
    mocks.handleSubmitConfirm.mockReset()
    mocks.handleClear.mockReset()
    mocks.openAiDialog.mockReset()
    mocks.openPrintPreview.mockReset()
    mocks.handleSaveAndNew.mockReset()
    mocks.focusFirstEditableCell.mockReset()
    mocks.handleSaveAndNew.mockResolvedValue(true)
  })

  it('保存草稿和智能识别导入按钮应联动到页面 composable', async () => {
    const wrapper = mountView()

    await wrapper.find('.save-draft-trigger').trigger('click')
    await wrapper.findAll('button').find(button => button.text().includes('智能识别导入')).trigger('click')

    expect(mocks.handleSubmit).toHaveBeenCalledTimes(1)
    expect(mocks.openAiDialog).toHaveBeenCalledTimes(1)
  })

  it('保存并新建成功后应聚焦第一条明细的首个可编辑单元格', async () => {
    const wrapper = mountView()

    await wrapper.findAll('button').find(button => button.text().includes('保存并新建')).trigger('click')
    await flushPromises()

    expect(mocks.handleSaveAndNew).toHaveBeenCalledTimes(1)
    expect(mocks.focusFirstEditableCell).toHaveBeenCalledTimes(1)
  })
})
