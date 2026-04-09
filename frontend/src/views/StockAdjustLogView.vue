<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">库存流水</div>
      </template>

      <el-form :inline="true" class="search-form">
        <el-form-item label="商品名称">
          <el-input
              v-model="searchForm.productName"
              placeholder="请输入商品名称"
              clearable
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
          v-loading="loading"
          :data="tableData"
          border
          stripe
          style="width: 100%"
      >
        <el-table-column prop="id" label="流水ID" width="90" />
        <el-table-column prop="productCode" label="商品编码" width="120" />
        <el-table-column prop="productName" label="商品名称" min-width="160" />
        <el-table-column prop="changeType" label="变更类型" width="140">
          <template #default="scope">
            <el-tag :type="getChangeTypeTag(scope.row.changeType)">
              {{ formatChangeType(scope.row.changeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="beforeQuantity" label="变更前库存" width="120" />
        <el-table-column prop="afterQuantity" label="变更后库存" width="120" />
        <el-table-column prop="changeQuantity" label="变动数量" width="120">
          <template #default="scope">
            <span :class="scope.row.changeQuantity >= 0 ? 'increase-text' : 'decrease-text'">
              {{ scope.row.changeQuantity }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="bizOrderNo" label="关联单号" min-width="160" />
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column label="备注" min-width="180">
          <template #default="scope">
            {{ scope.row.remark || scope.row.reason || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="操作时间" width="180" />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="handleCurrentChange"
            @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getStockAdjustLogList } from '../api/stockAdjustLog'

const searchForm = reactive({
  productName: ''
})

const tableData = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const formatChangeType = (changeType) => {
  if (changeType === 'MANUAL_INBOUND') return '手工入库'
  if (changeType === 'AI_CONFIRM_INBOUND') return 'AI确认入库'
  if (changeType === 'MANUAL_OUTBOUND') return '手工出库'
  if (changeType === 'AI_CONFIRM_OUTBOUND') return 'AI确认出库'
  if (changeType === 'VOID_INBOUND') return '作废入库回退'
  if (changeType === 'VOID_OUTBOUND') return '作废出库回补'
  if (changeType === 'MANUAL_ADJUST') return '手工调整'
  return changeType || '-'
}

const getChangeTypeTag = (changeType) => {
  if (changeType === 'MANUAL_INBOUND') return 'success'
  if (changeType === 'AI_CONFIRM_INBOUND') return 'warning'
  if (changeType === 'MANUAL_OUTBOUND') return 'danger'
  if (changeType === 'AI_CONFIRM_OUTBOUND') return 'danger'
  if (changeType === 'VOID_INBOUND') return 'danger'
  if (changeType === 'VOID_OUTBOUND') return 'success'
  if (changeType === 'MANUAL_ADJUST') return 'info'
  return ''
}

const loadLogList = async () => {
  loading.value = true
  try {
    const res = await getStockAdjustLogList({
      productName: searchForm.productName,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    const result = res.data

    if (result.code === 1) {
      tableData.value = result.data?.list || []
      total.value = result.data?.total || 0
    } else {
      ElMessage.error(result.message || '获取库存流水失败')
    }
  } catch (error) {
    ElMessage.error('请求库存流水失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.productName = ''
  pageNum.value = 1
  pageSize.value = 10
  loadLogList()
}

const handleSearch = () => {
  pageNum.value = 1
  loadLogList()
}

const handleCurrentChange = (val) => {
  pageNum.value = val
  loadLogList()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  pageNum.value = 1
  loadLogList()
}

onMounted(() => {
  loadLogList()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.card-header {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.search-form {
  margin-bottom: 20px;
}

.increase-text {
  color: #67c23a;
  font-weight: 600;
}

.decrease-text {
  color: #f56c6c;
  font-weight: 600;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
