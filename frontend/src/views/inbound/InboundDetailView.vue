<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>入库单详情</span>
          <div class="header-actions">
            <el-button
              v-if="isAdmin && canConfirm(detail.orderStatus)"
              type="success"
              @click="handleConfirm"
            >
              确认入库
            </el-button>
            <el-button
              v-if="isAdmin && canVoid(detail.orderStatus)"
              type="danger"
              plain
              @click="handleVoid"
            >
              作废
            </el-button>
            <el-button type="primary" plain @click="openPrintPage">打印单据</el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading">
        <el-descriptions title="单据信息" :column="2" border class="desc-area">
          <el-descriptions-item label="入库单号">
            {{ detail.orderNo || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="供应商">
            {{ detail.supplierName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="总金额">
            {{ detail.totalAmount ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="来源类型">
            <el-tag :type="getSourceTypeTagType(detail.sourceType)">
              {{ getSourceTypeText(detail.sourceType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(detail.orderStatus)">
              {{ getStatusText(detail.orderStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ detail.createdTime || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="备注">
            {{ detail.remark || '-' }}
          </el-descriptions-item>
          <template v-if="detail.sourceType === 'AI'">
            <el-descriptions-item label="AI记录ID">
              <el-button
                v-if="detail.aiRecordId"
                link
                type="primary"
                @click="handleViewAiRecord"
              >
                {{ detail.aiRecordId }}
              </el-button>
              <span v-else>-</span>
            </el-descriptions-item>
            <el-descriptions-item label="AI任务号">
              {{ detail.aiTaskNo || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="源文件名">
              {{ detail.aiSourceFileName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="来源追溯">
              <el-button
                v-if="detail.aiRecordId"
                type="primary"
                link
                @click="handleViewAiRecord"
              >
                查看AI记录
              </el-button>
              <span v-else>-</span>
            </el-descriptions-item>
          </template>
        </el-descriptions>

        <OrderDetailItemTable title="入库明细" :item-list="detail.itemList || []" />
        <OrderSummary :item-list="detail.itemList || []" />

        <div class="form-actions">
          <el-button @click="handleBack">返回</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { confirmInboundOrder, getInboundOrderDetail, voidInboundOrder } from '../../api/inbound'
import { getRole } from '../../utils/auth'
import OrderDetailItemTable from '../../components/order/OrderDetailItemTable.vue'
import OrderSummary from '../../components/order/OrderSummary.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const isAdmin = computed(() => getRole() === 'ADMIN')

const detail = reactive({
  orderNo: '',
  supplierName: '',
  totalAmount: null,
  orderStatus: null,
  sourceType: 'MANUAL',
  aiRecordId: null,
  aiTaskNo: '',
  aiSourceFileName: '',
  createdTime: '',
  remark: '',
  itemList: []
})

const getStatusText = (status) => {
  if (Number(status) === 2) return '已入库'
  if (Number(status) === 3) return '已作废'
  return '草稿'
}

const getStatusTagType = (status) => {
  if (Number(status) === 2) return 'success'
  if (Number(status) === 3) return 'info'
  return 'warning'
}

const getSourceTypeText = (sourceType) => {
  if (sourceType === 'AI') return 'AI识别生成'
  return '手工创建'
}

const getSourceTypeTagType = (sourceType) => {
  if (sourceType === 'AI') return 'warning'
  return 'info'
}

const canConfirm = (status) => Number(status) === 1

const canVoid = (status) => [1, 2].includes(Number(status))

const loadDetail = async () => {
  loading.value = true
  try {
    const id = route.params.id
    const res = await getInboundOrderDetail(id)

    if (res.data && res.data.code === 1) {
      const data = res.data.data || {}
      detail.orderNo = data.orderNo || ''
      detail.supplierName = data.supplierName || ''
      detail.totalAmount = data.totalAmount
      detail.orderStatus = data.orderStatus
      detail.sourceType = data.sourceType || 'MANUAL'
      detail.aiRecordId = data.aiRecordId ?? null
      detail.aiTaskNo = data.aiTaskNo || ''
      detail.aiSourceFileName = data.aiSourceFileName || ''
      detail.createdTime = data.createdTime || ''
      detail.remark = data.remark || ''
      detail.itemList = data.itemList || []
    } else {
      ElMessage.error(res.data?.message || '加载入库单详情失败')
    }
  } catch (error) {
    console.error('加载入库单详情失败：', error)
    ElMessage.error(error?.response?.data?.message || '请求入库单详情接口失败')
  } finally {
    loading.value = false
  }
}

const openPrintPage = () => {
  const id = route.params.id
  window.open(`/inbound/print/${id}`, '_blank')
}

const handleBack = () => {
  const query = {}
  if (route.query.orderNo) query.orderNo = String(route.query.orderNo)
  if (route.query.sourceType) query.sourceType = String(route.query.sourceType)
  if (route.query.orderStatus) query.orderStatus = String(route.query.orderStatus)
  if (route.query.pageNum) query.pageNum = String(route.query.pageNum)
  if (route.query.pageSize) query.pageSize = String(route.query.pageSize)
  router.push({ path: '/inbound/list', query })
}

const handleViewAiRecord = () => {
  if (!detail.aiRecordId) {
    return
  }

  router.push({
    path: '/ai/inbound/list',
    query: {
      recordId: String(detail.aiRecordId)
    }
  })
}

const handleConfirm = async () => {
  if (!isAdmin.value) {
    ElMessage.warning('仅管理员可执行确认入库')
    return
  }
  const id = route.params.id
  try {
    await ElMessageBox.confirm('确认后才会真正增加库存，是否继续确认入库？', '确认入库', {
      type: 'warning'
    })
    const res = await confirmInboundOrder(id)
    if (res.data?.code === 1) {
      ElMessage.success(res.data?.data || '确认入库成功')
      loadDetail()
    } else {
      ElMessage.error(res.data?.message || '确认入库失败')
    }
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    console.error('确认入库失败：', error)
    ElMessage.error(error?.response?.data?.message || error?.message || '确认入库失败')
  }
}

const handleVoid = async () => {
  if (!isAdmin.value) {
    ElMessage.warning('仅管理员可执行作废')
    return
  }
  const id = route.params.id
  const isCompleted = Number(detail.orderStatus) === 2
  const confirmMessage = isCompleted
    ? '作废后将自动回退本单据已增加的库存，是否继续？'
    : '作废后该草稿单据将不能再确认入库，是否继续？'
  const successText = isCompleted ? '作废入库单成功，库存已回退' : '作废入库单成功'
  try {
    const { value } = await ElMessageBox.prompt(confirmMessage, '作废入库单', {
      type: 'warning',
      inputPlaceholder: '请输入作废原因',
      inputValidator: (inputValue) => {
        if (!inputValue || !inputValue.trim()) {
          return '请输入作废原因'
        }
        return true
      }
    })
    const res = await voidInboundOrder(id, value.trim())
    if (res.data?.code === 1) {
      ElMessage.success(res.data?.data || successText)
      loadDetail()
    } else {
      ElMessage.error(res.data?.message || '作废入库单失败')
    }
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    console.error('作废入库单失败：', error)
    ElMessage.error(error?.response?.data?.message || error?.message || '作废入库单失败')
  }
}

onMounted(() => {
  loadDetail()
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

.header-actions {
  display: flex;
  gap: 12px;
}

.desc-area {
  margin-bottom: 20px;
}

.form-actions {
  margin-top: 20px;
}
</style>
