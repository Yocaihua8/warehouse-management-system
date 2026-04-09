<template>
  <PrintTemplate
    :loading="loading"
    title="销售出库单"
    warehouse-label="发货仓库"
    warehouse-name="总仓库"
    party-label="购货单位"
    :party-name="detail.customerName"
    :detail="detail"
    row-key="out-print"
  />
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { getOutboundDetail } from '../../api/outbound'
import PrintTemplate from '../../components/print/PrintTemplate.vue'

const route = useRoute()
const loading = ref(false)
const printed = ref(false)

const detail = reactive({
  orderNo: '',
  customerName: '',
  totalAmount: null,
  createdTime: '',
  remark: '',
  itemList: []
})

const triggerPrintOnce = async () => {
  if (printed.value) return
  printed.value = true
  await nextTick()
  setTimeout(() => window.print(), 200)
}

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await getOutboundDetail(route.params.id)
    if (res.data?.code === 1) {
      const data = res.data?.data || {}
      detail.orderNo = data.orderNo || ''
      detail.customerName = data.customerName || ''
      detail.totalAmount = data.totalAmount
      detail.createdTime = data.createdTime || ''
      detail.remark = data.remark || ''
      detail.itemList = data.itemList || []
      await triggerPrintOnce()
    } else {
      ElMessage.error(res.data?.message || '加载出库单详情失败')
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '请求出库单详情接口失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>
