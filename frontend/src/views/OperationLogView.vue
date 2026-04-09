<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">操作日志</div>
      </template>

      <el-form :inline="true" class="search-form">
        <el-form-item label="操作类型">
          <el-select v-model="searchForm.actionType" placeholder="全部" clearable style="width: 220px">
            <el-option label="登录成功" value="LOGIN_SUCCESS" />
            <el-option label="确认入库" value="INBOUND_CONFIRM" />
            <el-option label="作废入库" value="INBOUND_VOID" />
            <el-option label="确认出库" value="OUTBOUND_CONFIRM" />
            <el-option label="作废出库" value="OUTBOUND_VOID" />
            <el-option label="AI确认入库" value="AI_INBOUND_CONFIRM" />
            <el-option label="AI确认出库" value="AI_OUTBOUND_CONFIRM" />
          </el-select>
        </el-form-item>

        <el-form-item label="操作人">
          <el-input
              v-model="searchForm.operatorName"
              placeholder="请输入操作人"
              clearable
          />
        </el-form-item>

        <el-form-item label="关联单号">
          <el-input
              v-model="searchForm.bizNo"
              placeholder="请输入关联单号"
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
        <el-table-column prop="id" label="日志ID" width="90" />
        <el-table-column prop="actionType" label="操作类型" width="180">
          <template #default="scope">
            <el-tag type="info">{{ formatActionType(scope.row.actionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="moduleName" label="模块" width="140" />
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="bizNo" label="关联单号" min-width="180" />
        <el-table-column prop="resultStatus" label="结果" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.resultStatus === 'SUCCESS' ? 'success' : 'danger'">
              {{ scope.row.resultStatus || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="描述" min-width="220" />
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
import { getOperationLogList } from '../api/operationLog'

const searchForm = reactive({
  actionType: '',
  operatorName: '',
  bizNo: ''
})

const tableData = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const formatActionType = (actionType) => {
  if (actionType === 'LOGIN_SUCCESS') return '登录成功'
  if (actionType === 'INBOUND_CONFIRM') return '确认入库'
  if (actionType === 'INBOUND_VOID') return '作废入库'
  if (actionType === 'OUTBOUND_CONFIRM') return '确认出库'
  if (actionType === 'OUTBOUND_VOID') return '作废出库'
  if (actionType === 'AI_INBOUND_CONFIRM') return 'AI确认入库'
  if (actionType === 'AI_OUTBOUND_CONFIRM') return 'AI确认出库'
  return actionType || '-'
}

const loadLogList = async () => {
  loading.value = true
  try {
    const res = await getOperationLogList({
      actionType: searchForm.actionType,
      operatorName: searchForm.operatorName,
      bizNo: searchForm.bizNo,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    const result = res.data

    if (result.code === 1) {
      tableData.value = result.data?.list || []
      total.value = result.data?.total || 0
    } else {
      ElMessage.error(result.message || '获取操作日志失败')
    }
  } catch (error) {
    ElMessage.error('请求操作日志失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.actionType = ''
  searchForm.operatorName = ''
  searchForm.bizNo = ''
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

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>

