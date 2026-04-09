<template>
  <div class="summary-wrap">
    <el-tag type="info">明细行数：{{ lineCount }}</el-tag>
    <el-tag type="success">总数量：{{ totalQuantity }}</el-tag>
    <el-tag type="warning">总金额：{{ totalAmount }}</el-tag>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  itemList: {
    type: Array,
    default: () => []
  }
})

const lineCount = computed(() => (Array.isArray(props.itemList) ? props.itemList.length : 0))

const totalQuantity = computed(() => {
  return (props.itemList || []).reduce((sum, item) => {
    return sum + Number(item?.quantity || 0)
  }, 0)
})

const totalAmount = computed(() => {
  const amount = (props.itemList || []).reduce((sum, item) => {
    const quantity = Number(item?.quantity || 0)
    const unitPrice = Number(item?.unitPrice || 0)
    return sum + quantity * unitPrice
  }, 0)
  return amount.toFixed(2)
})
</script>

<style scoped>
.summary-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  margin-bottom: 6px;
}
</style>
