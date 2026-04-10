<template>
  <PrintTemplate
    :loading="loading"
    :print-data="printData"
    row-key="in-print"
    :detail-row-height="52"
    :detail-row-no-wrap="true"
  />
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { getInboundOrderDetail } from '../../api/inbound'
import PrintTemplate from '../../components/print/PrintTemplate.vue'
import { buildInboundPrintData } from '../../utils/printAdapter'
import { triggerBrowserPrint } from '../../utils/printService'

const route = useRoute()
const loading = ref(false)
const printed = ref(false)

const detail = reactive({
  orderNo: '',
  supplierName: '',
  totalAmount: null,
  sourceType: 'MANUAL',
  aiRecordId: null,
  aiTaskNo: '',
  aiSourceFileName: '',
  createdTime: '',
  remark: '',
  itemList: []
})

const printData = computed(() => buildInboundPrintData(detail))

const triggerPrintOnce = async () => {
  if (printed.value) return
  printed.value = true
  await triggerBrowserPrint(200)
}

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await getInboundOrderDetail(route.params.id)
    if (res.data?.code === 1) {
      const data = res.data?.data || {}
      detail.orderNo = data.orderNo || ''
      detail.supplierName = data.supplierName || ''
      detail.totalAmount = data.totalAmount
      detail.sourceType = data.sourceType || 'MANUAL'
      detail.aiRecordId = data.aiRecordId ?? null
      detail.aiTaskNo = data.aiTaskNo || ''
      detail.aiSourceFileName = data.aiSourceFileName || ''
      detail.createdTime = data.createdTime || ''
      detail.remark = data.remark || ''
      detail.itemList = data.itemList || []
      await triggerPrintOnce()
    } else {
      ElMessage.error(res.data?.message || '加载入库单详情失败')
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '请求入库单详情接口失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>
