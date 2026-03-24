<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>库存管理</span>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="商品编码">
          <el-input
              v-model="queryForm.productCode"
              placeholder="请输入商品编码"
              clearable
              style="width: 220px"
          />
        </el-form-item>

        <el-form-item label="商品名称">
          <el-input
              v-model="queryForm.productName"
              placeholder="请输入商品名称"
              clearable
              style="width: 220px"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            查询
          </el-button>
          <el-button @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <el-table
          v-loading="loading"
          :data="tableData"
          border
          stripe
          class="table-area"
      >
        <el-table-column prop="productId" label="商品ID" width="100" />
        <el-table-column prop="productCode" label="商品编码" width="160" />
        <el-table-column prop="productName" label="商品名称" min-width="180" />
        <el-table-column prop="specification" label="规格" width="140" />
        <el-table-column prop="unit" label="单位" width="100" />
        <el-table-column prop="category" label="分类" width="140" />
        <el-table-column prop="quantity" label="当前库存" width="120" />
        <el-table-column prop="warningQuantity" label="预警值" width="120" />
        <el-table-column prop="lowStock" label="低库存" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.lowStock === 1 ? 'danger' : 'success'">
              {{ scope.row.lowStock === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[5, 10, 20, 50]"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getStockList } from '../../api/stock'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const queryForm = reactive({
  productCode: '',
  productName: ''
})

const loadStockList = async () => {
  loading.value = true
  try {
    const res = await getStockList({
      productCode: queryForm.productCode,
      productName: queryForm.productName,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })

    if (res.data && res.data.code === 1) {
      const data = res.data.data

      if (Array.isArray(data)) {
        tableData.value = data
        total.value = data.length
      } else {
        tableData.value = data?.list || []
        total.value = data?.total || 0
      }
    } else {
      ElMessage.error(res.data?.message || '查询库存列表失败')
    }
  } catch (error) {
    console.error('加载库存列表失败:', error)
    ElMessage.error('请求库存列表接口失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadStockList()
}

const handleReset = () => {
  queryForm.productCode = ''
  queryForm.productName = ''
  pageNum.value = 1
  pageSize.value = 10
  loadStockList()
}

const handleCurrentChange = (currentPage) => {
  pageNum.value = currentPage
  loadStockList()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  pageNum.value = 1
  loadStockList()
}

onMounted(() => {
  loadStockList()
})
</script>

<style scoped>
.page-container {
  padding: 24px;
}

.page-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.query-form {
  margin-bottom: 16px;
}

.table-area {
  width: 100%;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

</style>