<template>
  <div class="table-toolbar">
    <span class="section-title">{{ title }}</span>
    <el-button type="primary" @click="emit('add-item')">新增明细</el-button>
  </div>

  <el-table :data="itemList" border style="width: 100%">
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
          style="width: 100%"
          :remote-method="(keyword) => emit('product-search', keyword)"
          @change="(value) => emit('product-change', { row, productId: value })"
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
          style="width: 100%"
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
          style="width: 100%"
        />
      </template>
    </el-table-column>

    <el-table-column label="金额" width="140">
      <template #default="{ row }">
        <span>{{ calcAmount(row) }}</span>
      </template>
    </el-table-column>

    <el-table-column label="备注" min-width="160">
      <template #default="{ row }">
        <el-input v-model="row.remark" placeholder="请输入备注" />
      </template>
    </el-table-column>

    <el-table-column label="操作" width="100" fixed="right">
      <template #default="{ $index }">
        <el-button
          type="danger"
          link
          @click="emit('remove-item', $index)"
          :disabled="itemList.length === 1"
        >
          删除
        </el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup>
import { displayText } from '../../utils/orderHelper'

defineProps({
  title: {
    type: String,
    default: '入库明细'
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
  }
})

const emit = defineEmits(['add-item', 'remove-item', 'product-search', 'product-change'])
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
