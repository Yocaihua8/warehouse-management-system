<template>
  <div class="page-container list-layout">
    <aside class="left-panel">
      <el-card shadow="never" class="side-card">
        <template #header>
          <span>导航与筛选</span>
        </template>
        <div class="side-block">
          <div class="side-title">快捷筛选</div>
          <el-button text type="primary" @click="applyQuickStatus(1)">只看草稿</el-button>
          <el-button text @click="applyQuickStatus('')">查看全部</el-button>
        </div>
        <div class="side-block">
          <div class="side-title">批量操作</div>
          <template v-if="isAdmin">
            <el-button type="success" plain size="small" :disabled="!canBatchConfirm" @click="handleBatchConfirm">批量确认</el-button>
            <el-button type="danger" plain size="small" :disabled="!canBatchVoid" @click="handleBatchVoid">批量作废</el-button>
          </template>
          <el-tag v-else type="info" size="small">操作员只读</el-tag>
          <div class="side-tip">已选 {{ selectedRows.length }} 条</div>
        </div>
      </el-card>
    </aside>

    <div class="main-panel">
      <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>出库单列表</span>
          <el-button type="primary" @click="handleAdd">
            新增出库单
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="出库单号">
          <el-input
              v-model="queryForm.orderNo"
              placeholder="请输入出库单号"
              clearable
              style="width: 220px"
          />
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="queryForm.orderStatus" placeholder="全部" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="草稿" :value="1" />
            <el-option label="已出库" :value="2" />
            <el-option label="已作废" :value="3" />
          </el-select>
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
          empty-text="暂无数据"
          @selection-change="handleSelectionChange"
          class="table-area"
      >
        <el-table-column type="selection" width="44" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="orderNo" label="出库单号" min-width="180" />
        <el-table-column prop="customerName" label="客户名称" min-width="180" />
        <el-table-column label="总金额" width="130" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.totalAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="orderStatus" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusTagType(scope.row.orderStatus)">
              {{ getStatusText(scope.row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="180" />
        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button
              v-if="canEdit(scope.row.orderStatus)"
              type="primary"
              link
              @click="handleEdit(scope.row.id)"
            >
              编辑草稿
            </el-button>
            <el-button
              v-if="isAdmin && canConfirm(scope.row.orderStatus)"
              type="success"
              link
              @click="handleConfirm(scope.row.id)"
            >
              确认出库
            </el-button>
            <el-button
              v-if="isAdmin && canVoid(scope.row.orderStatus)"
              type="danger"
              link
              @click="handleVoid(scope.row)"
            >
              作废
            </el-button>
            <el-button type="info" link @click="handleDetail(scope.row.id)">
              详情
            </el-button>
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
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { confirmOutboundOrder, getOutboundList, voidOutboundOrder } from '../../api/outbound'
import { getRole } from '../../utils/auth'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])
const isAdmin = computed(() => getRole() === 'ADMIN')

const queryForm = reactive({
  orderNo: '',
  orderStatus: ''
})

const parsePageData = (payload) => {
  if (Array.isArray(payload)) {
    return {
      list: payload,
      total: payload.length
    }
  }

  return {
    list: Array.isArray(payload?.list) ? payload.list : [],
    total: typeof payload?.total === 'number' ? payload.total : 0
  }
}

const loadOutboundList = async () => {
  loading.value = true
  try {
    const res = await getOutboundList({
      orderNo: queryForm.orderNo,
      orderStatus: queryForm.orderStatus === '' ? undefined : Number(queryForm.orderStatus),
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })

    if (res.data && res.data.code === 1) {
      const pageData = parsePageData(res.data.data)
      tableData.value = pageData.list
      total.value = pageData.total
    } else {
      tableData.value = []
      total.value = 0
      ElMessage.error(res.data?.message || '加载出库单列表失败')
    }
  } catch (error) {
    tableData.value = []
    total.value = 0
    console.error('加载出库单列表失败：', error)
    ElMessage.error(error?.response?.data?.message || error?.message || '加载出库单列表失败')
  } finally {
    loading.value = false
  }
}

const getStatusText = (status) => {
  if (Number(status) === 2) return '已出库'
  if (Number(status) === 3) return '已作废'
  return '草稿'
}

const formatAmount = (value) => {
  const amount = Number(value)
  if (!Number.isFinite(amount)) {
    return '0.00'
  }
  return amount.toFixed(2)
}

const getStatusTagType = (status) => {
  if (Number(status) === 2) return 'success'
  if (Number(status) === 3) return 'info'
  return 'warning'
}

const canConfirm = (status) => Number(status) === 1

const canVoid = (status) => [1, 2].includes(Number(status))

const canEdit = (status) => Number(status) === 1
const canBatchConfirm = computed(() =>
  selectedRows.value.length > 0 && selectedRows.value.every(row => canConfirm(row.orderStatus))
)
const canBatchVoid = computed(() =>
  selectedRows.value.length > 0 && selectedRows.value.every(row => canVoid(row.orderStatus))
)

const buildListQuery = () => {
  const query = {
    pageNum: String(pageNum.value),
    pageSize: String(pageSize.value)
  }
  if (queryForm.orderNo) query.orderNo = queryForm.orderNo
  if (queryForm.orderStatus !== '') query.orderStatus = String(queryForm.orderStatus)
  return query
}

const applyRouteQuery = (query) => {
  queryForm.orderNo = typeof query.orderNo === 'string' ? query.orderNo : ''
  queryForm.orderStatus = query.orderStatus === undefined ? '' : Number(query.orderStatus)
  pageNum.value = Number(query.pageNum) > 0 ? Number(query.pageNum) : 1
  pageSize.value = Number(query.pageSize) > 0 ? Number(query.pageSize) : 10
}

const replaceRouteQuery = async () => {
  await router.replace({ path: route.path, query: buildListQuery() })
}

const handleSearch = async () => {
  pageNum.value = 1
  await replaceRouteQuery()
}

const handleReset = async () => {
  queryForm.orderNo = ''
  queryForm.orderStatus = ''
  pageNum.value = 1
  pageSize.value = 10
  await replaceRouteQuery()
}

const handleAdd = () => {
  router.push('/outbound/create')
}

const handleDetail = (id) => {
  router.push({ path: `/outbound/detail/${id}`, query: buildListQuery() })
}

const handleEdit = (id) => {
  router.push({ path: '/outbound/create', query: { draftId: id } })
}

const handleConfirm = async (id) => {
  if (!isAdmin.value) {
    ElMessage.warning('仅管理员可执行确认出库')
    return
  }
  try {
    await ElMessageBox.confirm('确认后才会真正扣减库存，是否继续确认出库？', '确认出库', {
      type: 'warning'
    })
    const res = await confirmOutboundOrder(id)
    if (res.data?.code === 1) {
      ElMessage.success(res.data?.data || '确认出库成功')
      loadOutboundList()
    } else {
      ElMessage.error(res.data?.message || '确认出库失败')
    }
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    console.error('确认出库失败：', error)
    ElMessage.error(error?.response?.data?.message || error?.message || '确认出库失败')
  }
}

const handleSelectionChange = (rows) => {
  selectedRows.value = Array.isArray(rows) ? rows : []
}

const applyQuickStatus = async (status) => {
  queryForm.orderStatus = status
  pageNum.value = 1
  await replaceRouteQuery()
}

const handleBatchConfirm = async () => {
  if (!isAdmin.value) {
    ElMessage.warning('仅管理员可执行批量确认')
    return
  }
  if (!canBatchConfirm.value) {
    ElMessage.warning('仅可批量确认草稿单')
    return
  }
  const selectedSnapshot = [...selectedRows.value]
  const result = await executeBatchAction(
    selectedSnapshot,
    (row) => confirmOutboundOrder(row.id),
    '批量确认'
  )
  notifyBatchResult('批量确认', result)
  selectedRows.value = []
  loadOutboundList()
}

const handleBatchVoid = async () => {
  if (!isAdmin.value) {
    ElMessage.warning('仅管理员可执行批量作废')
    return
  }
  if (!canBatchVoid.value) {
    ElMessage.warning('仅可批量作废草稿/已出库单')
    return
  }
  try {
    const { value } = await ElMessageBox.prompt('请输入批量作废原因', '批量作废', {
      type: 'warning',
      inputPlaceholder: '请输入作废原因',
      inputValidator: (inputValue) => {
        if (!inputValue || !inputValue.trim()) {
          return '请输入作废原因'
        }
        return true
      }
    })
    const reason = value.trim()
    const selectedSnapshot = [...selectedRows.value]
    const result = await executeBatchAction(
      selectedSnapshot,
      (row) => voidOutboundOrder(row.id, reason),
      '批量作废'
    )
    notifyBatchResult('批量作废', result)
    selectedRows.value = []
    loadOutboundList()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error?.message || '批量作废失败')
  }
}

const extractApiMessage = (error, fallback) => {
  if (!error) return fallback
  if (error?.response?.data?.message) return error.response.data.message
  if (error?.message) return error.message
  return fallback
}

const executeBatchAction = async (rows, action, actionName) => {
  const detail = []
  for (const row of rows) {
    try {
      const res = await action(row)
      if (res.data?.code === 1) {
        detail.push({ id: row.id, orderNo: row.orderNo, success: true, message: res.data?.data || '' })
      } else {
        detail.push({
          id: row.id,
          orderNo: row.orderNo,
          success: false,
          message: res.data?.message || `${actionName}失败`
        })
      }
    } catch (error) {
      detail.push({
        id: row.id,
        orderNo: row.orderNo,
        success: false,
        message: extractApiMessage(error, `${actionName}失败`)
      })
    }
  }
  const successCount = detail.filter(item => item.success).length
  const failItems = detail.filter(item => !item.success)
  return { total: rows.length, successCount, failItems }
}

const notifyBatchResult = (actionName, result) => {
  const failCount = result.failItems.length
  if (failCount === 0) {
    ElMessage.success(`${actionName}完成：成功 ${result.successCount} 条`)
    return
  }

  const failedPreview = result.failItems
    .slice(0, 3)
    .map(item => `${item.orderNo || item.id}（${item.message}）`)
    .join('；')

  if (result.successCount === 0) {
    ElMessage.error(`${actionName}失败：${failedPreview}`)
    return
  }
  ElMessage.warning(`${actionName}部分成功：成功 ${result.successCount} 条，失败 ${failCount} 条。失败示例：${failedPreview}`)
}

const handleVoid = async (rowOrId) => {
  if (!isAdmin.value) {
    ElMessage.warning('仅管理员可执行作废')
    return
  }
  const row = typeof rowOrId === 'object' ? rowOrId : { id: rowOrId, orderStatus: 1 }
  const isCompleted = Number(row.orderStatus) === 2
  const confirmMessage = isCompleted
    ? '作废后将自动回补本单据已扣减的库存，是否继续？'
    : '作废后该草稿单据将不能再确认出库，是否继续？'
  const successText = isCompleted ? '作废出库单成功，库存已回补' : '作废出库单成功'
  try {
    const { value } = await ElMessageBox.prompt(confirmMessage, '作废出库单', {
      type: 'warning',
      inputPlaceholder: '请输入作废原因',
      inputValidator: (inputValue) => {
        if (!inputValue || !inputValue.trim()) {
          return '请输入作废原因'
        }
        return true
      }
    })
    const res = await voidOutboundOrder(row.id, value.trim())
    if (res.data?.code === 1) {
      ElMessage.success(res.data?.data || successText)
      loadOutboundList()
    } else {
      ElMessage.error(res.data?.message || '作废出库单失败')
    }
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    console.error('作废出库单失败：', error)
    ElMessage.error(error?.response?.data?.message || error?.message || '作废出库单失败')
  }
}

const handleCurrentChange = async (currentPage) => {
  pageNum.value = currentPage
  await replaceRouteQuery()
}

const handleSizeChange = async (size) => {
  pageSize.value = size
  pageNum.value = 1
  await replaceRouteQuery()
}

watch(
  () => route.query,
  async (query) => {
    applyRouteQuery(query)
    await loadOutboundList()
  },
  { immediate: true }
)
</script>

<style scoped>
.page-container {
  padding: 24px;
}

.list-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.left-panel {
  width: 220px;
  position: sticky;
  top: 16px;
}

.main-panel {
  flex: 1;
  min-width: 0;
}

.side-card {
  border-radius: 12px;
}

.side-block + .side-block {
  margin-top: 16px;
}

.side-title {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}

.side-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
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
