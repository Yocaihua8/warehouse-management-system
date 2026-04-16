<template>
  <div v-if="editable" class="product-select-cell">
    <el-input
      :model-value="selectedText"
      readonly
      placeholder="点击右侧按钮选择商品"
      @click="emit('open')"
    >
      <template #append>
        <el-button :tabindex="-1" :disabled="productLoading" @click="emit('open')">
          选择
        </el-button>
      </template>
    </el-input>
    <el-button
      link
      type="danger"
      :tabindex="-1"
      :disabled="!hasSelectedProduct"
      @click="emit('clear')"
    >
      清空
    </el-button>
  </div>
  <span v-else>{{ selectedText }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  row: {
    type: Object,
    default: () => ({})
  },
  editable: {
    type: Boolean,
    default: true
  },
  productLoading: {
    type: Boolean,
    default: false
  },
  selectedText: {
    type: String,
    default: '未选择商品'
  }
})

const emit = defineEmits(['open', 'clear'])

const hasSelectedProduct = computed(() => Boolean(props.row?.productId))
</script>

<style scoped>
.product-select-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
