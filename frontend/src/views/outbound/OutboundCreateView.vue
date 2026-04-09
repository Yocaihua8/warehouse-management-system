<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>{{ pageTitle }}</span>
          <el-button type="primary" plain @click="openAiDialog">智能识别导入</el-button>
        </div>
      </template>

      <el-form :model="form" label-width="100px" class="outbound-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="客户" required>
              <el-select v-model="form.customerId" placeholder="请选择客户" filterable style="width: 100%">
                <el-option
                  v-for="item in customerOptions"
                  :key="item.id"
                  :label="item.customerName"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>

        <OrderItemTable
          title="出库明细"
          :item-list="form.itemList"
          :product-options="productOptions"
          :product-loading="false"
          :calc-amount="calcAmount"
          @add-item="handleAddItem"
          @remove-item="handleRemoveItem"
          @product-search="() => {}"
          @product-change="onProductChange"
        />

        <div class="form-actions">
          <el-button @click="handleBack">返回</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ currentDraftId ? '更新草稿' : '保存草稿' }}</el-button>
        </div>
      </el-form>
      <OrderSummary :item-list="form.itemList" />
    </el-card>

    <AiOutboundRecognitionDialog ref="aiDialogRef" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { getCustomerList } from '../../api/customer'
import { getProductList } from '../../api/product'
import { addOutboundOrder, getOutboundDetail, updateOutboundOrderDraft } from '../../api/outbound'
import { useOrderForm } from '../../composables/useOrderForm'
import { useOrderItems } from '../../composables/useOrderItems'
import { useOrderCalc } from '../../composables/useOrderCalc'
import { useOrderValidation } from '../../composables/useOrderValidation'
import { parsePageData } from '../../utils/orderHelper'
import OrderItemTable from '../../components/order/OrderItemTable.vue'
import AiOutboundRecognitionDialog from '../../components/order/AiOutboundRecognitionDialog.vue'
import OrderSummary from '../../components/order/OrderSummary.vue'

const route = useRoute()
const router = useRouter()

const aiDialogRef = ref(null)
const submitting = ref(false)
const currentDraftId = ref(null)
const customerOptions = ref([])
const productOptions = ref([])
const initialSnapshot = ref('')
const pageTitle = computed(() => currentDraftId.value ? '编辑出库草稿' : '新增出库单')

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
  customerId: null,
  remark: '',
  itemList: [createEmptyItem()]
})

const { addItem: handleAddItem, removeItem: handleRemoveItem } = useOrderItems({
  form,
  createEmptyItem,
  minItems: 1
})
const { calcAmount } = useOrderCalc()
const { resetForm, parseDraftId } = useOrderForm({
  form,
  createEmptyItem,
  defaultValues: { customerId: null, remark: '' }
})
const { validateOutboundForm } = useOrderValidation((message) => ElMessage.warning(message))

const loadCustomers = async () => {
  try {
    const res = await getCustomerList({ pageNum: 1, pageSize: 200 })
    customerOptions.value = res.data?.code === 1 ? parsePageData(res.data.data).list : []
    if (res.data?.code !== 1) {
      ElMessage.error(res.data?.message || '加载客户列表失败')
    }
  } catch (error) {
    customerOptions.value = []
    ElMessage.error(error?.response?.data?.message || error?.message || '请求客户列表接口失败')
  }
}

const loadProducts = async () => {
  try {
    const res = await getProductList({ pageNum: 1, pageSize: 200 })
    productOptions.value = res.data?.code === 1 ? parsePageData(res.data.data).list : []
    if (res.data?.code !== 1) {
      ElMessage.error(res.data?.message || '加载商品列表失败')
    }
  } catch (error) {
    productOptions.value = []
    ElMessage.error(error?.response?.data?.message || error?.message || '请求商品列表接口失败')
  }
}

const onProductChange = ({ row, productId }) => {
  const selected = productOptions.value.find(item => Number(item.id) === Number(productId))
  if (!selected) {
    row.productCode = ''
    row.productName = ''
    row.specification = ''
    row.unit = ''
    return
  }
  row.productCode = selected.productCode || ''
  row.productName = selected.productName || ''
  row.specification = selected.specification || ''
  row.unit = selected.unit || ''
}

const handleBack = () => router.push('/outbound/list')

const buildFormSnapshot = () => {
  return JSON.stringify({
    customerId: form.customerId != null ? Number(form.customerId) : null,
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

const loadOutboundDraft = async (draftIdValue) => {
  const draftId = parseDraftId(draftIdValue)
  if (!draftId) {
    currentDraftId.value = null
    return
  }

  try {
    const res = await getOutboundDetail(draftId)
    if (res.data?.code !== 1) {
      ElMessage.error(res.data?.message || '加载出库草稿失败')
      return
    }

    const detail = res.data?.data
    if (!detail) {
      ElMessage.error('出库草稿不存在')
      return
    }
    if (Number(detail.orderStatus) !== 1) {
      ElMessage.warning('仅草稿状态允许编辑，已为你跳转到详情页')
      await router.replace(`/outbound/detail/${draftId}`)
      return
    }

    currentDraftId.value = draftId
    form.customerId = detail.customerId != null ? Number(detail.customerId) : null
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
    ElMessage.error(error?.response?.data?.message || error?.message || '加载出库草稿失败')
  }
}

const handleSubmit = async () => {
  if (!validateOutboundForm(form)) {
    return
  }

  try {
    submitting.value = true
    const payload = {
      customerId: form.customerId,
      remark: form.remark,
      itemList: form.itemList.map(item => ({
        productId: item.productId,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        remark: item.remark
      }))
    }

    const isEditMode = !!currentDraftId.value
    const res = isEditMode
      ? await updateOutboundOrderDraft(currentDraftId.value, payload)
      : await addOutboundOrder(payload)

    if (res.data?.code === 1) {
      if (isEditMode) {
        ElMessage.success(res.data?.data || '更新出库草稿成功')
      } else {
        const createdOrder = res.data?.data
        const orderNo = createdOrder?.orderNo
        const orderId = createdOrder?.id
        ElMessage.success(orderNo
          ? `保存出库单草稿成功，单号：${orderNo}`
          : (orderId ? `保存出库单草稿成功，ID=${orderId}` : '保存出库单草稿成功'))
      }
      resetForm()
      refreshSnapshot()
      currentDraftId.value = null
      await router.push('/outbound/list')
    } else {
      ElMessage.error(res.data?.message || '保存出库草稿失败')
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '保存出库草稿失败')
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
    await loadOutboundDraft(draftId)
  },
  { immediate: true }
)

onMounted(() => {
  loadCustomers()
  loadProducts()
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

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.outbound-form {
  max-width: 1100px;
}

.form-actions {
  margin-top: 24px;
}
</style>
