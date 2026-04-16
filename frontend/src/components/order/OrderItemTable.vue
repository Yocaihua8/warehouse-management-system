<template>
  <div v-if="showToolbar" class="table-toolbar">
    <span class="section-title">{{ title }}</span>
    <el-button v-if="showAddButton" type="primary" :disabled="!editable" @click="emitRowAdd">新增明细</el-button>
  </div>

  <el-table
    :data="resolvedItems"
    border
    :stripe="stripe"
    style="width: 100%"
    :show-summary="showSummary"
    :summary-method="buildSummaryRow"
    :row-class-name="resolveRowClassName"
  >
    <el-table-column v-if="showIndexColumn" label="#" width="60" align="center">
      <template #default="{ $index }">
        {{ $index + 1 }}
      </template>
    </el-table-column>

    <el-table-column v-if="showProductSelectColumn" label="商品选择" min-width="260">
      <template #default="{ row, $index }">
        <div
          v-if="editable"
          :ref="setCellRef($index, 'productSelect')"
          class="cell-focus-wrapper"
          @focusin.capture="setActiveRow($index)"
          @click.capture="setActiveRow($index)"
          @keydown.capture="handleCellKeydown($event, $index, 'productSelect')"
          @keydown.enter.prevent="emitOpenProductDialog($index)"
        >
          <OrderProductSelectCell
            :row="row"
            :editable="true"
            :product-loading="productLoading"
            :selected-text="formatSelectedProduct(row)"
            @open="emitOpenProductDialog($index)"
            @clear="clearProduct(row, $index)"
          />
        </div>
        <OrderProductSelectCell
          v-else
          :row="row"
          :editable="false"
          :selected-text="formatSelectedProduct(row)"
        />
      </template>
    </el-table-column>

    <el-table-column label="商品编码" min-width="140">
      <template #default="{ row, $index }">
        <div
          v-if="editable"
          :ref="setCellRef($index, 'productCode')"
          class="cell-focus-wrapper"
          @focusin.capture="setActiveRow($index)"
          @click.capture="setActiveRow($index)"
          @keydown.capture="handleCellKeydown($event, $index, 'productCode')"
        >
          <el-input
            v-model="row.productCode"
            placeholder="商品编码"
            @input="(value) => handleRowFieldChange($index, 'productCode', value)"
          />
        </div>
        <span v-else>{{ displayText(row.productCode) }}</span>
      </template>
    </el-table-column>

    <el-table-column label="商品名称" min-width="180">
      <template #default="{ row, $index }">
        <div
          v-if="editable"
          :ref="setCellRef($index, 'productName')"
          class="cell-focus-wrapper"
          @focusin.capture="setActiveRow($index)"
          @click.capture="setActiveRow($index)"
          @keydown.capture="handleCellKeydown($event, $index, 'productName')"
        >
          <el-input
            v-model="row.productName"
            placeholder="商品名称"
            @input="(value) => handleRowFieldChange($index, 'productName', value)"
          />
        </div>
        <span v-else>{{ displayText(row.productName) }}</span>
      </template>
    </el-table-column>

    <el-table-column label="规格" min-width="140">
      <template #default="{ row, $index }">
        <div
          v-if="editable"
          :ref="setCellRef($index, 'specification')"
          class="cell-focus-wrapper"
          @focusin.capture="setActiveRow($index)"
          @click.capture="setActiveRow($index)"
          @keydown.capture="handleCellKeydown($event, $index, 'specification')"
        >
          <el-input
            v-model="row.specification"
            placeholder="规格"
            @input="(value) => handleRowFieldChange($index, 'specification', value)"
          />
        </div>
        <span v-else>{{ displayText(row.specification) }}</span>
      </template>
    </el-table-column>

    <el-table-column label="单位" width="90" align="center">
      <template #default="{ row, $index }">
        <div
          v-if="editable"
          :ref="setCellRef($index, 'unit')"
          class="cell-focus-wrapper"
          @focusin.capture="setActiveRow($index)"
          @click.capture="setActiveRow($index)"
          @keydown.capture="handleCellKeydown($event, $index, 'unit')"
        >
          <el-input
            v-model="row.unit"
            placeholder="单位"
            @input="(value) => handleRowFieldChange($index, 'unit', value)"
          />
        </div>
        <span v-else>{{ displayText(row.unit) }}</span>
      </template>
    </el-table-column>

    <el-table-column label="数量" width="140">
      <template #default="{ row, $index }">
        <div
          :ref="setCellRef($index, 'quantity')"
          class="cell-focus-wrapper"
          @focusin.capture="setActiveRow($index)"
          @click.capture="setActiveRow($index)"
          @keydown.capture="handleCellKeydown($event, $index, 'quantity')"
        >
          <el-input-number
            v-model="row.quantity"
            :min="1"
            controls-position="right"
            :disabled="!editable"
            style="width: 100%"
            @change="(value) => handleRowFieldChange($index, 'quantity', value)"
          />
        </div>
      </template>
    </el-table-column>

    <el-table-column label="单价" width="160">
      <template #default="{ row, $index }">
        <div
          :ref="setCellRef($index, 'unitPrice')"
          class="cell-focus-wrapper"
          @focusin.capture="setActiveRow($index)"
          @click.capture="setActiveRow($index)"
          @keydown.capture="handleCellKeydown($event, $index, 'unitPrice')"
        >
          <el-input-number
            v-model="row.unitPrice"
            :min="0"
            :precision="2"
            controls-position="right"
            :disabled="!editable"
            style="width: 100%"
            @change="(value) => handleRowFieldChange($index, 'unitPrice', value)"
          />
        </div>
      </template>
    </el-table-column>

    <el-table-column label="金额" width="140">
      <template #default="{ row }">
        <span>{{ calcAmount(row) }}</span>
      </template>
    </el-table-column>

    <el-table-column v-if="orderType === 'outbound' && showStockColumn" label="库存余量" width="120">
      <template #default="{ row }">
        {{ displayText(row.availableStock) }}
      </template>
    </el-table-column>

    <el-table-column label="备注" min-width="160">
      <template #default="{ row, $index }">
        <div
          :ref="setCellRef($index, 'remark')"
          class="cell-focus-wrapper"
          @focusin.capture="setActiveRow($index)"
          @click.capture="setActiveRow($index)"
          @keydown.capture="handleCellKeydown($event, $index, 'remark')"
        >
          <el-input v-model="row.remark" :disabled="!editable" placeholder="请输入备注" @input="(value) => handleRowFieldChange($index, 'remark', value)" />
        </div>
      </template>
    </el-table-column>

    <el-table-column v-if="showActionColumn" label="操作" width="100" fixed="right">
      <template #default="{ $index }">
        <el-button
          type="danger"
          link
          :disabled="!editable || resolvedItems.length === 1"
          @click="emitRowDelete($index)"
        >
          删除
        </el-button>
        <el-button
          type="primary"
          link
          :disabled="!editable"
          @click="emitRowInsert($index)"
        >
          插入
        </el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup>
import { computed } from 'vue'
import { useOrderItemTableFocus } from '../../composables/useOrderItemTableFocus'
import { displayText } from '../../utils/orderHelper'
import OrderProductSelectCell from './OrderProductSelectCell.vue'

const props = defineProps({
  title: {
    type: String,
    default: '入库明细'
  },
  items: {
    type: Array,
    default: () => []
  },
  itemList: {
    type: Array,
    default: () => []
  },
  productOptions: {
    type: Array,
    default: () => []
  },
  productLoading: {
    type: Boolean,
    default: false
  },
  showToolbar: {
    type: Boolean,
    default: true
  },
  showAddButton: {
    type: Boolean,
    default: true
  },
  showIndexColumn: {
    type: Boolean,
    default: true
  },
  showProductSelectColumn: {
    type: Boolean,
    default: true
  },
  showActionColumn: {
    type: Boolean,
    default: true
  },
  showSummary: {
    type: Boolean,
    default: true
  },
  showStockColumn: {
    type: Boolean,
    default: true
  },
  stripe: {
    type: Boolean,
    default: false
  },
  calcAmount: {
    type: Function,
    required: true
  },
  editable: {
    type: Boolean,
    default: true
  },
  orderType: {
    type: String,
    default: 'inbound'
  }
})

const emit = defineEmits([
  'row-add',
  'row-insert',
  'row-delete',
  'add-item',
  'insert-item',
  'remove-item',
  'row-field-change',
  'open-product-dialog',
  'product-change',
  'product-selected'
])

const resolvedItems = computed(() => {
  if (Array.isArray(props.items) && props.items.length > 0) {
    return props.items
  }
  return props.itemList
})

const totalQuantity = computed(() => {
  return (resolvedItems.value || []).reduce((sum, item) => sum + Number(item?.quantity || 0), 0)
})

const totalAmount = computed(() => {
  const total = (resolvedItems.value || []).reduce((sum, item) => {
    const quantity = Number(item?.quantity || 0)
    const unitPrice = Number(item?.unitPrice || 0)
    return sum + quantity * unitPrice
  }, 0)
  return total.toFixed(2)
})

const buildSummaryRow = ({ columns }) => {
  return columns.map((column) => {
    const label = column?.label || ''
    if (label === '#') {
      return '合计'
    }
    if (label === '数量') {
      return String(totalQuantity.value)
    }
    if (label === '金额') {
      return totalAmount.value
    }
    return ''
  })
}

const emitRowAdd = () => {
  emit('row-add')
  emit('add-item')
}

const emitRowInsert = (index) => {
  emit('row-insert', index)
  emit('insert-item', index)
}

const emitRowDelete = (index) => {
  emit('row-delete', index)
  emit('remove-item', index)
}

const emitOpenProductDialog = (index) => {
  emit('open-product-dialog', index)
}

const handleProductChange = (row, productId, index) => {
  emit('product-selected', { row, productId, index })
  emit('product-change', { row, productId, index })
}

const clearProduct = (row, index) => {
  handleProductChange(row, null, index)
}

const formatSelectedProduct = (row) => {
  if (!row?.productId && !row?.productCode && !row?.productName) {
    return '未选择商品'
  }
  const code = row?.productCode || '-'
  const name = row?.productName || '-'
  const specification = row?.specification ? ` / ${row.specification}` : ''
  return `${code} / ${name}${specification}`
}

const handleRowFieldChange = (index, field, value) => {
  emit('row-field-change', { index, field, value })
}

const {
  setCellRef,
  setActiveRow,
  resolveRowClassName,
  focusFirstEditableCell,
  handleCellKeydown
} = useOrderItemTableFocus({
  editable: computed(() => props.editable),
  resolvedItems,
  onAppendRow: emitRowAdd
})

defineExpose({
  focusFirstEditableCell
})
</script>

<style scoped>
.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
}

.cell-focus-wrapper {
  width: 100%;
}

:deep(.el-table .is-active-row > td.el-table__cell) {
  background-color: var(--el-color-primary-light-9);
}

:deep(.el-table .is-active-row:hover > td.el-table__cell) {
  background-color: var(--el-color-primary-light-8);
}
</style>
