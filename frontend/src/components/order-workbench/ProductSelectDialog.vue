<template>
  <el-dialog
    title="选择商品"
    :model-value="visible"
    width="860px"
    @close="$emit('update:visible', false)"
  >
    <div class="product-select-toolbar">
      <el-input
        :model-value="keyword"
        placeholder="输入商品编码或名称搜索"
        clearable
        @input="(value) => $emit('search', value)"
      >
        <template #append>
          <el-button @click="$emit('search', keyword)">搜索</el-button>
        </template>
      </el-input>
      <el-button plain @click="$emit('quick-create', keyword)">快速新建商品</el-button>
    </div>

    <el-table
      :data="products"
      height="420"
      border
      highlight-current-row
      :current-row-key="selectedProductId"
      row-key="id"
      v-loading="loading"
      @row-click="(row) => $emit('select', row)"
      @row-dblclick="(row) => $emit('confirm', row)"
    >
      <el-table-column prop="productCode" label="商品编码" width="140" />
      <el-table-column prop="productName" label="商品名称" min-width="180" />
      <el-table-column prop="specification" label="规格" min-width="120" />
      <el-table-column prop="unit" label="单位" width="80" />
      <el-table-column prop="salePrice" label="默认单价" width="120" />
    </el-table>

    <template #footer>
      <div class="product-select-footer">
        <div class="product-select-current">
          已选商品：{{ currentProductLabel }}
        </div>
        <div class="product-select-actions">
          <el-button @click="$emit('update:visible', false)">取消</el-button>
          <el-button
            type="primary"
            :disabled="!currentProduct"
            @click="$emit('confirm', currentProduct)"
          >
            确认
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  keyword: {
    type: String,
    default: ''
  },
  products: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  selectedProductId: {
    type: [Number, String, null],
    default: null
  }
})

defineEmits(['update:visible', 'search', 'select', 'confirm', 'quick-create'])

const currentProduct = computed(() => {
  return (props.products || []).find(item => Number(item.id) === Number(props.selectedProductId)) || null
})

const currentProductLabel = computed(() => {
  if (!currentProduct.value) {
    return '未选择'
  }
  const { productCode, productName, specification } = currentProduct.value
  return `${productCode || '-'} / ${productName || '-'}${specification ? ` / ${specification}` : ''}`
})
</script>

<style scoped>
.product-select-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.product-select-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.product-select-current {
  color: #4b5563;
  font-size: 14px;
}

.product-select-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>
