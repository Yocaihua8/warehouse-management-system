<template>
  <section class="order-detail-table">
    <OrderItemTable
      ref="itemTableRef"
      :title="title"
      :items="items"
      :order-type="orderType"
      :editable="editable"
      :product-options="productOptions"
      :product-loading="productLoading"
      :calc-amount="calcAmount"
      @row-add="(index) => emit('row-add', index)"
      @row-insert="(index) => emit('row-insert', index)"
      @row-delete="(index) => emit('row-delete', index)"
      @row-field-change="(payload) => emit('row-field-change', payload)"
      @product-selected="(payload) => emit('product-selected', payload)"
      @open-product-dialog="openProductDialog"
    />

    <ProductSelectDialog
      :visible="dialogVisible"
      :keyword="dialogKeyword"
      :products="productOptions"
      :loading="productLoading"
      :selected-product-id="selectedProductId"
      @update:visible="handleDialogVisibleChange"
      @search="handleDialogSearch"
      @select="handleDialogSelect"
      @confirm="handleDialogConfirm"
      @quick-create="(keyword) => emit('quick-create', { keyword, index: activeRowIndex })"
    />
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import OrderItemTable from '../order/OrderItemTable.vue'
import ProductSelectDialog from './ProductSelectDialog.vue'

const props = defineProps({
  orderType: {
    type: String,
    default: 'inbound'
  },
  items: {
    type: Array,
    default: () => []
  },
  editable: {
    type: Boolean,
    default: true
  },
  productOptions: {
    type: Array,
    default: () => []
  },
  productLoading: {
    type: Boolean,
    default: false
  },
  calcAmount: {
    type: Function,
    required: true
  }
})

const emit = defineEmits([
  'row-add',
  'row-insert',
  'row-delete',
  'row-field-change',
  'product-search',
  'product-selected',
  'quick-create'
])

const title = computed(() => {
  return props.orderType === 'outbound' ? '出库明细' : '入库明细'
})

const itemTableRef = ref(null)
const dialogVisible = ref(false)
const dialogKeyword = ref('')
const activeRowIndex = ref(null)
const selectedProductId = ref(null)

const focusFirstEditableCell = async () => {
  await itemTableRef.value?.focusFirstEditableCell?.()
}

const resetDialogState = () => {
  dialogKeyword.value = ''
  activeRowIndex.value = null
  selectedProductId.value = null
}

const openProductDialog = (index) => {
  const nextIndex = Number(index)
  const row = props.items?.[nextIndex]
  activeRowIndex.value = Number.isFinite(nextIndex) ? nextIndex : null
  selectedProductId.value = row?.productId ?? null
  dialogKeyword.value = ''
  dialogVisible.value = true
}

const handleDialogVisibleChange = (visible) => {
  dialogVisible.value = visible
  if (!visible) {
    resetDialogState()
  }
}

const handleDialogSearch = (keyword) => {
  dialogKeyword.value = keyword || ''
  emit('product-search', dialogKeyword.value)
}

const handleDialogSelect = (product) => {
  selectedProductId.value = product?.id ?? null
}

const handleDialogConfirm = (product) => {
  if (activeRowIndex.value == null || !product?.id) {
    return
  }
  const row = props.items?.[activeRowIndex.value]
  emit('product-selected', {
    row,
    productId: product.id,
    index: activeRowIndex.value
  })
  dialogVisible.value = false
  resetDialogState()
}

defineExpose({
  focusFirstEditableCell
})
</script>

<style scoped>
.order-detail-table {
  border: 1px solid #e8edf4;
  border-radius: 10px;
  padding: 18px;
  background: #fff;
}
</style>
