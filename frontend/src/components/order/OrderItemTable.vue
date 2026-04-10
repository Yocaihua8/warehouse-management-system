<template>
  <div class="table-toolbar">
    <span class="section-title">{{ title }}</span>
    <el-button type="primary" :disabled="!editable" @click="emitRowAdd">新增明细</el-button>
  </div>

  <el-table
    :data="resolvedItems"
    border
    style="width: 100%"
    show-summary
    :summary-method="buildSummaryRow"
  >
    <el-table-column label="#" width="60" align="center">
      <template #default="{ $index }">
        {{ $index + 1 }}
      </template>
    </el-table-column>

    <el-table-column label="商品选择" min-width="260">
      <template #default="{ row }">
        <el-select
          v-model="row.productId"
          placeholder="请选择商品"
          filterable
          remote
          reserve-keyword
          clearable
          :loading="productLoading"
          :disabled="!editable"
          style="width: 100%"
          :remote-method="emitProductSearch"
          @change="(value) => handleProductChange(row, value, $index)"
        >
          <el-option
            v-for="item in productOptions"
            :key="item.id"
            :label="`${item.productCode || '-'} / ${item.productName || '-'}${item.specification ? ' / ' + item.specification : ''}`"
            :value="item.id"
          />
        </el-select>
      </template>
    </el-table-column>

    <el-table-column label="商品编码" min-width="140">
      <template #default="{ row }">
        {{ displayText(row.productCode) }}
      </template>
    </el-table-column>

    <el-table-column label="商品名称" min-width="180">
      <template #default="{ row }">
        {{ displayText(row.productName) }}
      </template>
    </el-table-column>

    <el-table-column label="规格" min-width="140">
      <template #default="{ row }">
        {{ displayText(row.specification) }}
      </template>
    </el-table-column>

    <el-table-column label="单位" width="90" align="center">
      <template #default="{ row }">
        {{ displayText(row.unit) }}
      </template>
    </el-table-column>

    <el-table-column label="数量" width="140">
      <template #default="{ row }">
        <el-input-number
          v-model="row.quantity"
          :min="1"
          controls-position="right"
          :disabled="!editable"
          style="width: 100%"
          @change="(value) => handleRowFieldChange($index, 'quantity', value)"
        />
      </template>
    </el-table-column>

    <el-table-column label="单价" width="160">
      <template #default="{ row }">
        <el-input-number
          v-model="row.unitPrice"
          :min="0"
          :precision="2"
          controls-position="right"
          :disabled="!editable"
          style="width: 100%"
          @change="(value) => handleRowFieldChange($index, 'unitPrice', value)"
        />
      </template>
    </el-table-column>

    <el-table-column label="金额" width="140">
      <template #default="{ row }">
        <span>{{ calcAmount(row) }}</span>
      </template>
    </el-table-column>

    <el-table-column v-if="orderType === 'outbound'" label="库存余量" width="120">
      <template #default="{ row }">
        {{ displayText(row.availableStock) }}
      </template>
    </el-table-column>

    <el-table-column label="备注" min-width="160">
      <template #default="{ row, $index }">
        <el-input v-model="row.remark" :disabled="!editable" placeholder="请输入备注" @input="(value) => handleRowFieldChange($index, 'remark', value)" />
      </template>
    </el-table-column>

    <el-table-column label="操作" width="100" fixed="right">
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
import { displayText } from '../../utils/orderHelper'

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
  'product-search',
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

const emitProductSearch = (keyword) => {
  emit('product-search', keyword)
}

const handleProductChange = (row, productId, index) => {
  emit('product-selected', { row, productId, index })
  emit('product-change', { row, productId, index })
}

const handleRowFieldChange = (index, field, value) => {
  emit('row-field-change', { index, field, value })
}
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
</style>
