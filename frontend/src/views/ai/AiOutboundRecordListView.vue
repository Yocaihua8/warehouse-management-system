<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>AI出库识别历史</span>
          <el-button type="primary" @click="loadData">刷新</el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        style="width: 100%"
      >
        <el-table-column prop="id" label="记录ID" width="90" />
        <el-table-column prop="taskNo" label="任务编号" min-width="220" />
        <el-table-column prop="sourceFileName" label="文件名" min-width="220" />
        <el-table-column prop="customerName" label="客户" min-width="180" />
        <el-table-column label="客户匹配" width="140">
          <template #default="{ row }">
            <el-tag :type="getCustomerMatchTagType(row.customerMatchStatus)">
              {{ formatCustomerMatchStatus(row.customerMatchStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="识别状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.recognitionStatus)">
              {{ formatStatus(row.recognitionStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="正式出库单号" min-width="180">
          <template #default="{ row }">
            <el-button
              v-if="row.confirmedOrderId"
              link
              type="success"
              @click="handleViewOutboundOrder(row.confirmedOrderId)"
            >
              {{ row.confirmedOrderNo || `ID:${row.confirmedOrderId}` }}
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDetail(row.id)">
              查看明细
            </el-button>
            <el-button
              v-if="canContinueConfirm(row)"
              link
              type="warning"
              @click="handleContinueConfirm(row.id)"
            >
              继续确认
            </el-button>
            <el-button
              v-if="row.confirmedOrderId"
              link
              type="success"
              @click="handleViewOutboundOrder(row.confirmedOrderId)"
            >
              查看出库单
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="detailDialogVisible"
      title="AI出库识别详情"
      width="900px"
      destroy-on-close
      @closed="handleDetailDialogClosed"
    >
      <div v-loading="detailLoading">
        <template v-if="detailData">
          <el-descriptions :column="2" border style="margin-bottom: 16px">
            <el-descriptions-item label="记录ID">
              {{ detailData.recordId }}
            </el-descriptions-item>
            <el-descriptions-item label="任务编号">
              {{ detailData.taskNo }}
            </el-descriptions-item>
            <el-descriptions-item label="文件名">
              {{ detailData.sourceFileName }}
            </el-descriptions-item>
            <el-descriptions-item label="客户名称">
              {{ detailData.customerName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="匹配客户ID">
              {{ detailData.matchedCustomerId || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="客户匹配">
              <el-tag :type="getCustomerMatchTagType(detailData.customerMatchStatus)">
                {{ formatCustomerMatchStatus(detailData.customerMatchStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="识别状态">
              <el-tag :type="getStatusTagType(detailData.recognitionStatus)">
                {{ formatStatus(detailData.recognitionStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="警告信息">
              {{ formatWarningText(detailData.warnings, detailData.warningsJson) }}
            </el-descriptions-item>
            <el-descriptions-item label="关联出库单ID">
              <template v-if="detailData.confirmedOrderId">
                <el-button
                  link
                  type="primary"
                  @click="handleViewOutboundOrder(detailData.confirmedOrderId)"
                >
                  {{ detailData.confirmedOrderId }}
                </el-button>
              </template>
              <span v-else>-</span>
            </el-descriptions-item>
            <el-descriptions-item label="正式出库单号">
              <template v-if="detailData.confirmedOrderId">
                <el-button
                  link
                  type="primary"
                  @click="handleViewOutboundOrder(detailData.confirmedOrderId)"
                >
                  {{ detailData.confirmedOrderNo || '-' }}
                </el-button>
              </template>
              <span v-else>-</span>
            </el-descriptions-item>
          </el-descriptions>

          <div class="detail-block">
            <div class="detail-title">原始文本</div>
            <el-input
              :model-value="detailData.rawText || ''"
              type="textarea"
              :rows="5"
              readonly
            />
          </div>

          <div class="detail-block">
            <div class="detail-title">识别明细</div>
            <el-table :data="detailData.itemList || []" border style="width: 100%">
              <el-table-column prop="lineNo" label="行号" width="80" />
              <el-table-column prop="productName" label="商品名称" min-width="160" />
              <el-table-column prop="specification" label="规格" width="120" />
              <el-table-column prop="unit" label="单位" width="80" />
              <el-table-column prop="quantity" label="数量" width="100" />
              <el-table-column prop="unitPrice" label="单价" width="120" />
              <el-table-column prop="amount" label="金额" width="120" />
              <el-table-column prop="matchedProductId" label="匹配商品ID" width="120" />
              <el-table-column prop="matchStatus" label="匹配状态" width="120" />
              <el-table-column prop="remark" label="备注" min-width="120" />
            </el-table>
          </div>
        </template>
      </div>

      <template #footer>
        <el-button
          v-if="canContinueConfirm(detailData)"
          type="primary"
          plain
          @click="handleContinueConfirm(detailData.recordId)"
        >
          继续人工确认
        </el-button>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { getAiOutboundRecordList, getAiOutboundRecordDetail } from '../../api/ai'

const loading = ref(false)
const tableData = ref([])
const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)
const route = useRoute()
const router = useRouter()

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAiOutboundRecordList()
    if (res.data?.code === 1) {
      tableData.value = res.data?.data || []
    } else {
      tableData.value = []
      ElMessage.error(res.data?.message || '加载AI出库识别历史失败')
    }
  } catch (error) {
    tableData.value = []
    console.error('加载AI出库识别历史失败：', error)
    ElMessage.error(error?.response?.data?.message || error?.message || '加载AI出库识别历史失败')
  } finally {
    loading.value = false
  }
}

const formatStatus = (status) => {
  if (status === 'confirmed') return '已确认生成出库单'
  if (status === 'success') return '待人工确认'
  if (status === 'failed') return '识别失败'
  if (status === 'pending') return '待处理'
  return status || '-'
}

const getStatusTagType = (status) => {
  if (status === 'confirmed') return 'success'
  if (status === 'success') return 'warning'
  if (status === 'failed') return 'danger'
  return 'info'
}

const formatCustomerMatchStatus = (status) => {
  if (status === 'matched_exact') return '精确匹配'
  if (status === 'matched_fuzzy') return '模糊匹配'
  if (status === 'manual_confirmed') return '人工确认'
  if (status === 'manual_selected') return '人工选择'
  if (status === 'manual_created') return '新建后回填'
  if (status === 'unmatched') return '未匹配'
  return status || '-'
}

const getCustomerMatchTagType = (status) => {
  if (status === 'matched_exact') return 'success'
  if (status === 'matched_fuzzy') return 'warning'
  if (status === 'manual_confirmed') return 'primary'
  if (status === 'manual_selected') return 'primary'
  if (status === 'manual_created') return 'primary'
  if (status === 'unmatched') return 'danger'
  return 'info'
}

const canContinueConfirm = (record) => {
  return Boolean(record) &&
    !record.confirmedOrderId &&
    record.recognitionStatus === 'success'
}

const openRecordDetail = async (id) => {
  if (!id) return

  detailDialogVisible.value = true
  detailLoading.value = true
  detailData.value = null

  try {
    const res = await getAiOutboundRecordDetail(id)
    if (res.data?.code === 1) {
      detailData.value = res.data?.data || null
    } else {
      detailData.value = null
      ElMessage.error(res.data?.message || '加载识别详情失败')
    }
  } catch (error) {
    detailData.value = null
    console.error('加载识别详情失败：', error)
    ElMessage.error(error?.response?.data?.message || error?.message || '加载识别详情失败')
  } finally {
    detailLoading.value = false
  }
}

const handleViewDetail = async (id) => {
  await openRecordDetail(id)
}

const formatWarningText = (warnings, warningsJson) => {
  if (Array.isArray(warnings) && warnings.length > 0) {
    return warnings.join('；')
  }

  if (!warningsJson) return '-'

  try {
    const arr = JSON.parse(warningsJson)
    return Array.isArray(arr) ? arr.join('；') : warningsJson
  } catch (e) {
    return warningsJson
  }
}

const handleViewOutboundOrder = (orderId) => {
  if (!orderId) return
  detailDialogVisible.value = false
  router.push(`/outbound/detail/${orderId}`)
}

const handleContinueConfirm = (recordId) => {
  if (!recordId) {
    return
  }
  detailDialogVisible.value = false
  router.push({
    path: '/outbound/create',
    query: {
      aiRecordId: String(recordId)
    }
  })
}

const handleDetailDialogClosed = async () => {
  detailData.value = null
  if (route.path !== '/ai/outbound/list' || !route.query.recordId) {
    return
  }
  await router.replace({ path: '/ai/outbound/list' })
}

watch(
  () => route.query.recordId,
  async (recordId) => {
    const normalizedRecordId = Number(recordId)
    if (!Number.isFinite(normalizedRecordId) || normalizedRecordId <= 0) {
      return
    }
    await openRecordDetail(normalizedRecordId)
  },
  { immediate: true }
)

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.page-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.detail-block {
  margin-top: 16px;
}

.detail-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}
</style>
