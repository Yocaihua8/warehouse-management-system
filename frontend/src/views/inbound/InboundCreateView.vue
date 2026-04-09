<template>
  <div class="page-container">
    <el-card class="page-card" shadow="never">
      <template #header>
        <div class="header-row">
          <span class="page-title">{{ pageTitle }}</span>
          <div class="header-actions">
            <el-button type="primary" plain @click="openAiDialog">智能识别导入</el-button>
            <el-button
              type="success"
              :loading="submitting"
              native-type="button"
              @click="handleSubmit"
            >
              {{ currentDraftId ? '更新草稿' : '保存草稿' }}
            </el-button>
          </div>
        </div>
      </template>

      <el-form :model="form" label-width="100px" class="base-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="供应商名称">
              <el-input v-model="form.supplierName" placeholder="请输入供应商名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <OrderItemTable
        :item-list="form.itemList"
        :product-options="productOptions"
        :product-loading="productLoading"
        :calc-amount="calcAmount"
        @add-item="addItem"
        @remove-item="removeItem"
        @product-search="handleProductSearch"
        @product-change="onProductChange"
      />
      <OrderSummary :item-list="form.itemList" />
    </el-card>

    <AiRecognitionDialog ref="aiDialogRef" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { getInboundOrderDetail, saveInboundOrder, updateInboundOrderDraft } from '../../api/inbound'
import { getProductList } from '../../api/product'
import { useOrderCalc } from '../../composables/useOrderCalc'
import { useOrderForm } from '../../composables/useOrderForm'
import { useOrderItems } from '../../composables/useOrderItems'
import { useOrderValidation } from '../../composables/useOrderValidation'
import { useProductSearch } from '../../composables/useProductSearch'
import OrderItemTable from '../../components/order/OrderItemTable.vue'
import AiRecognitionDialog from '../../components/order/AiRecognitionDialog.vue'
import OrderSummary from '../../components/order/OrderSummary.vue'

const route = useRoute()
const router = useRouter()

const aiDialogRef = ref(null)
const submitting = ref(false)
const currentDraftId = ref(null)
const initialSnapshot = ref('')
const pageTitle = computed(() => (currentDraftId.value ? '编辑入库草稿' : '新增入库单'))

const createEmptyItem = () => ({
  productId: null,
  productCode: '',
  productName: '',
  specification: '',
  unit: '',
  quantity: 1,
  unitPrice: 0,
  remark: ''
})

const form = reactive({
  supplierName: '',
  remark: '',
  itemList: [createEmptyItem()]
})

const { calcAmount } = useOrderCalc()
const { addItem, removeItem } = useOrderItems({
  form,
  createEmptyItem,
  minItems: 1,
  onMinItemsReached: () => ElMessage.warning('至少保留一条入库明细')
})
const { validateInboundForm } = useOrderValidation((message) => ElMessage.error(message))
const { resetForm, parseDraftId } = useOrderForm({
  form,
  createEmptyItem,
  defaultValues: { supplierName: '', remark: '' }
})

const {
  productOptions,
  productLoading,
  loadProducts: loadProductOptions,
  handleProductSearch,
  handleProductChange
} = useProductSearch({
  fetchProductList: getProductList,
  onError: (message) => ElMessage.error(message)
})

const onProductChange = ({ row, productId }) => {
  handleProductChange(row, productId)
}

const buildPayload = () => ({
  supplierName: form.supplierName.trim(),
  remark: form.remark,
  itemList: form.itemList.map(item => ({
    productId: Number(item.productId),
    quantity: Number(item.quantity),
    unitPrice: Number(item.unitPrice),
    remark: item.remark
  }))
})

const buildFormSnapshot = () => {
  return JSON.stringify({
    supplierName: (form.supplierName || '').trim(),
    remark: form.remark || '',
    itemList: (form.itemList || []).map(item => ({
      productId: item.productId != null ? Number(item.productId) : null,
      quantity: Number(item.quantity || 0),
      unitPrice: Number(item.unitPrice || 0),
      remark: item.remark || ''
    }))
  })
}

const refreshSnapshot = () => {
  initialSnapshot.value = buildFormSnapshot()
}

const hasUnsavedChanges = computed(() => buildFormSnapshot() !== initialSnapshot.value)

const loadInboundDraft = async (draftIdValue) => {
  const draftId = parseDraftId(draftIdValue)
  if (!draftId) {
    currentDraftId.value = null
    return
  }

  try {
    const res = await getInboundOrderDetail(draftId)
    if (res.data?.code !== 1) {
      ElMessage.error(res.data?.message || '加载入库草稿失败')
      return
    }

    const detail = res.data?.data
    if (!detail) {
      ElMessage.error('入库草稿不存在')
      return
    }
    if (Number(detail.orderStatus) !== 1) {
      ElMessage.warning('仅草稿状态允许编辑，已为你跳转到详情页')
      await router.replace(`/inbound/detail/${draftId}`)
      return
    }

    currentDraftId.value = draftId
    form.supplierName = detail.supplierName || ''
    form.remark = detail.remark || ''
    form.itemList = Array.isArray(detail.itemList) && detail.itemList.length > 0
      ? detail.itemList.map(item => ({
        productId: item.productId != null ? Number(item.productId) : null,
        productCode: item.productCode || '',
        productName: item.productName || '',
        specification: item.specification || '',
        unit: item.unit || '',
        quantity: Number(item.quantity || 1),
        unitPrice: Number(item.unitPrice || 0),
        remark: item.remark || ''
      }))
      : [createEmptyItem()]
    refreshSnapshot()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '加载入库草稿失败')
  }
}

const handleSubmit = async () => {
  if (!validateInboundForm(form)) {
    return
  }

  try {
    submitting.value = true
    const payload = buildPayload()
    const isEditMode = !!currentDraftId.value
    const res = isEditMode
      ? await updateInboundOrderDraft(currentDraftId.value, payload)
      : await saveInboundOrder(payload)

    if (res.data?.code === 1) {
      if (isEditMode) {
        ElMessage.success(res.data?.data || '更新草稿成功')
      } else {
        const createdOrder = res.data?.data
        const orderNo = createdOrder?.orderNo
        const orderId = createdOrder?.id
        ElMessage.success(orderNo
          ? `保存草稿成功，单号：${orderNo}`
          : (orderId ? `保存草稿成功，ID=${orderId}` : '保存草稿成功'))
      }
      resetForm()
      refreshSnapshot()
      currentDraftId.value = null
      await router.push('/inbound/list')
    } else {
      ElMessage.error(res.data?.message || '保存失败')
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

const openAiDialog = () => {
  aiDialogRef.value?.openDialog?.()
}

const handleBeforeUnload = (event) => {
  if (!hasUnsavedChanges.value) {
    return
  }
  event.preventDefault()
  event.returnValue = ''
}

onBeforeRouteLeave(async () => {
  if (!hasUnsavedChanges.value || submitting.value) {
    return true
  }
  try {
    await ElMessageBox.confirm('当前草稿有未保存修改，确认离开吗？', '离开提示', {
      type: 'warning',
      confirmButtonText: '离开',
      cancelButtonText: '继续编辑'
    })
    return true
  } catch (error) {
    return false
  }
})

watch(
  () => route.query.draftId,
  async (draftId) => {
    if (!draftId) {
      currentDraftId.value = null
      refreshSnapshot()
      return
    }
    await loadInboundDraft(draftId)
  },
  { immediate: true }
)

onMounted(() => {
  loadProductOptions()
  refreshSnapshot()
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<style scoped>
.page-container {
  padding: 24px;
}

.page-card {
  border-radius: 12px;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.base-form {
  margin-bottom: 24px;
}
</style>
