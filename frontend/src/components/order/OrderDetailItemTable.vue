<template>
  <div class="table-title">{{ title }}</div>
  <el-table :data="itemList || []" border stripe class="table-area">
    <el-table-column prop="productCode" label="商品编码" width="160" />
    <el-table-column prop="productName" label="商品名称" min-width="180" />
    <el-table-column prop="specification" label="规格" width="140" />
    <el-table-column prop="unit" label="单位" width="100" />
    <el-table-column prop="quantity" label="数量" width="100" />
    <el-table-column prop="unitPrice" label="单价" width="120" />
    <el-table-column prop="amount" label="金额" width="120" />
    <el-table-column prop="remark" label="备注" min-width="180" />
  </el-table>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: '明细'
  },
  itemList: {
    type: Array,
    default: () => []
  }
})
</script>

<style scoped>
.table-title {
  margin: 12px 0;
  font-size: 16px;
  font-weight: 600;
}

.table-area {
  width: 100%;
}
</style>
