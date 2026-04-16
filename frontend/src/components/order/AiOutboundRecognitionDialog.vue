<template>
  <el-dialog
    v-model="aiDialogVisible"
    title="AI智能识别导入"
    width="960px"
    destroy-on-close
    @closed="handleAiDialogClosed"
  >
    <div class="ai-upload-block">
      <el-upload
        :auto-upload="false"
        :show-file-list="true"
        :limit="1"
        :before-upload="beforeAiUpload"
        :on-change="handleAiFileChange"
        :on-remove="handleAiFileRemove"
        accept=".jpg,.jpeg,.png"
      >
        <el-button type="primary">选择文件</el-button>
      </el-upload>

      <el-button
        type="success"
        :loading="aiRecognizing"
        :disabled="!aiUploadFile"
        @click="handleAiRecognize"
      >
        开始识别
      </el-button>
    </div>

    <div v-if="aiDraft" class="ai-result-block">
      <el-alert
        title="识别完成，请确认草稿后生成正式出库单"
        type="success"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />

      <el-alert
        v-if="getAiWarningText()"
        title="识别警告"
        :description="getAiWarningText()"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />

      <el-form label-width="100px" class="base-form">
        <el-form-item label="客户名称">
          <el-input v-model="aiDraft.customerName" @input="markAiDraftDirty" />
        </el-form-item>

        <el-form-item label="单据备注">
          <el-input v-model="aiDraft.remark" @input="markAiDraftDirty" />
        </el-form-item>

        <el-form-item label="匹配客户">
          <div class="match-cell">
            <el-select
              v-model="aiDraft.matchedCustomerId"
              placeholder="请选择客户"
              filterable
              clearable
              style="flex: 1"
              @change="handleMatchedCustomerChange"
            >
              <el-option
                v-for="item in customerOptions"
                :key="item.id"
                :label="`${item.customerCode} / ${item.customerName}`"
                :value="item.id"
              />
            </el-select>
            <el-button
              v-if="!aiDraft.matchedCustomerId"
              type="primary"
              link
              @click="openQuickCreateCustomer"
            >
              新增客户
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="原始文本">
          <el-input
            v-model="aiDraft.rawText"
            type="textarea"
            :rows="4"
            @input="markAiDraftDirty"
          />
        </el-form-item>
      </el-form>

      <el-table :data="aiDraft.itemList || []" border style="width: 100%">
        <el-table-column label="行号" width="100">
          <template #default="{ row }">
            <el-input-number
              v-model="row.lineNo"
              :min="1"
              controls-position="right"
              style="width: 100%"
              @change="markAiDraftDirty"
            />
          </template>
        </el-table-column>
        <el-table-column label="商品名称" min-width="180">
          <template #default="{ row }">
            <el-input v-model="row.productName" @input="markAiDraftDirty" />
          </template>
        </el-table-column>
        <el-table-column label="规格" width="140">
          <template #default="{ row }">
            <el-input v-model="row.specification" @input="markAiDraftDirty" />
          </template>
        </el-table-column>
        <el-table-column label="单位" width="100">
          <template #default="{ row }">
            <el-input v-model="row.unit" @input="markAiDraftDirty" />
          </template>
        </el-table-column>
        <el-table-column label="数量" width="120">
          <template #default="{ row }">
            <el-input-number
              v-model="row.quantity"
              :min="1"
              controls-position="right"
              style="width: 100%"
              @change="markAiDraftDirty"
            />
          </template>
        </el-table-column>
        <el-table-column label="单价" width="140">
          <template #default="{ row }">
            <el-input-number
              v-model="row.unitPrice"
              :min="0"
              :precision="2"
              controls-position="right"
              style="width: 100%"
              @change="markAiDraftDirty"
            />
          </template>
        </el-table-column>
        <el-table-column label="金额" width="140">
          <template #default="{ row }">
            <el-input-number
              v-model="row.amount"
              :min="0"
              :precision="2"
              controls-position="right"
              style="width: 100%"
              @change="markAiDraftDirty"
            />
          </template>
        </el-table-column>
        <el-table-column label="匹配商品" min-width="260">
          <template #default="{ row }">
            <div class="match-cell">
              <el-select
                v-model="row.matchedProductId"
                placeholder="请选择商品"
                filterable
                clearable
                style="flex: 1"
                @change="(value) => handleMatchedProductChange(row, value)"
              >
                <el-option
                  v-for="item in productOptions"
                  :key="item.id"
                  :label="`${item.productCode} / ${item.productName}`"
                  :value="item.id"
                />
              </el-select>
              <el-button type="primary" link @click="openQuickCreateProduct(row)">
                新增商品
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="matchStatus" label="匹配状态" width="120" />
        <el-table-column label="备注" min-width="160">
          <template #default="{ row }">
            <el-input v-model="row.remark" @input="markAiDraftDirty" />
          </template>
        </el-table-column>
      </el-table>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="aiDialogVisible = false">取消</el-button>
        <el-checkbox v-model="aiManualReviewed" :disabled="!aiDraft">
          我已人工校对识别结果
        </el-checkbox>
        <el-button
          type="primary"
          :loading="aiConfirming"
          :disabled="!aiDraft || !aiManualReviewed || aiConfirming"
          @click="handleAiConfirm"
        >
          确认生成出库单
        </el-button>
      </div>
    </template>
  </el-dialog>

  <QuickCreateDialog
    v-model:visible="quickCreateCustomerVisible"
    title="快速新建客户"
    :loading="quickCreateCustomerLoading"
    confirm-text="保存客户"
    @confirm="handleQuickCreateCustomer"
  >
    <el-form label-width="90px">
      <el-form-item label="客户编码">
        <el-input v-model="quickCreateCustomerForm.customerCode" />
      </el-form-item>
      <el-form-item label="客户名称">
        <el-input v-model="quickCreateCustomerForm.customerName" />
      </el-form-item>
      <el-form-item label="联系人">
        <el-input v-model="quickCreateCustomerForm.contactPerson" />
      </el-form-item>
      <el-form-item label="联系电话">
        <el-input v-model="quickCreateCustomerForm.phone" />
      </el-form-item>
      <el-form-item label="地址">
        <el-input v-model="quickCreateCustomerForm.address" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="quickCreateCustomerForm.remark" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
  </QuickCreateDialog>

  <QuickCreateDialog
    v-model:visible="quickCreateProductVisible"
    title="快速新建商品"
    :loading="quickCreateProductLoading"
    confirm-text="保存商品"
    @confirm="handleQuickCreateProduct"
  >
    <el-form label-width="90px">
      <el-form-item label="商品编码">
        <el-input v-model="quickCreateProductForm.productCode" />
      </el-form-item>
      <el-form-item label="商品名称">
        <el-input v-model="quickCreateProductForm.productName" />
      </el-form-item>
      <el-form-item label="规格">
        <el-input v-model="quickCreateProductForm.specification" />
      </el-form-item>
      <el-form-item label="单位">
        <el-input v-model="quickCreateProductForm.unit" />
      </el-form-item>
      <el-form-item label="分类">
        <el-input v-model="quickCreateProductForm.category" />
      </el-form-item>
      <el-form-item label="价格">
        <el-input-number
          v-model="quickCreateProductForm.salePrice"
          :min="0"
          :precision="2"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="quickCreateProductForm.remark" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
  </QuickCreateDialog>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { getCustomerList, addCustomer } from '../../api/customer'
import { getProductList, addProduct } from '../../api/product'
import { confirmOutbound, getAiOutboundRecordDetail, recognizeOutbound } from '../../api/ai'
import { useAiRecognition } from '../../composables/useAiRecognition'
import { useQuickCreate } from '../../composables/useQuickCreate'
import { parsePageData } from '../../utils/orderHelper'
import QuickCreateDialog from './QuickCreateDialog.vue'

const route = useRoute()
const router = useRouter()

const MAX_AI_FILE_SIZE = 20 * 1024 * 1024

const {
  aiDialogVisible,
  aiRecognizing,
  aiConfirming,
  aiUploadFile,
  aiDraft,
  aiManualReviewed,
  normalizeAiDraft,
  markAiDraftDirty,
  getAiWarningText,
  hasUnmatchedAiItems,
  hasInvalidAiItems,
  buildAiConfirmPayload,
  resetAiDraftState
} = useAiRecognition({
  confirmRemark: 'AI识别确认生成出库单',
  normalizeDraft: (draft, ctx) => {
    if (!draft) {
      return null
    }
    const mappedItems = Array.isArray(draft.itemList)
      ? draft.itemList.map((item, index) => ctx.normalizeItem(item, index))
      : []
    return {
      ...draft,
      customerName: draft.customerName || '',
      rawText: draft.rawText || '',
      remark: draft.remark || ctx.confirmRemark,
      itemList: mappedItems
    }
  },
  buildConfirmPayload: (draft, ctx) => ({
    recordId: draft?.recordId,
    customerId: draft?.matchedCustomerId != null ? Number(draft.matchedCustomerId) : null,
    customerName: draft?.customerName || '',
    rawText: draft?.rawText || '',
    remark: draft?.remark || ctx.confirmRemark,
    itemList: (draft?.itemList || []).map(item => ({
      lineNo: Number(item.lineNo),
      productName: item.productName || '',
      specification: item.specification || '',
      unit: item.unit || '',
      matchedProductId: item.matchedProductId != null ? Number(item.matchedProductId) : null,
      quantity: Number(item.quantity),
      unitPrice: Number(item.unitPrice),
      amount: Number(item.amount),
      remark: item.remark || ''
    }))
  })
})

const customerOptions = ref([])
const productOptions = ref([])

const { buildTempCode, createQuickCreateState } = useQuickCreate()
const quickCreateCustomerState = createQuickCreateState({
  customerCode: '',
  customerName: '',
  contactPerson: '',
  phone: '',
  address: '',
  remark: ''
})

const quickCreateProductState = createQuickCreateState({
  productCode: '',
  productName: '',
  specification: '',
  unit: '',
  category: '',
  salePrice: 0,
  remark: ''
})
const quickCreateCustomerVisible = quickCreateCustomerState.visible
const quickCreateCustomerLoading = quickCreateCustomerState.loading
const quickCreateCustomerForm = quickCreateCustomerState.form
const quickCreateProductVisible = quickCreateProductState.visible
const quickCreateProductLoading = quickCreateProductState.loading
const quickCreateProductRow = quickCreateProductState.targetRow
const quickCreateProductForm = quickCreateProductState.form

const loadCustomers = async () => {
  try {
    const res = await getCustomerList({ pageNum: 1, pageSize: 200 })
    if (res.data?.code === 1) {
      customerOptions.value = parsePageData(res.data?.data).list
    } else {
      customerOptions.value = []
      ElMessage.error(res.data?.message || '加载客户列表失败')
    }
  } catch (error) {
    customerOptions.value = []
    ElMessage.error(error?.response?.data?.message || error?.message || '加载客户列表失败')
  }
}

const loadProducts = async () => {
  try {
    const res = await getProductList({ pageNum: 1, pageSize: 200 })
    if (res.data?.code === 1) {
      productOptions.value = parsePageData(res.data?.data).list
    } else {
      productOptions.value = []
      ElMessage.error(res.data?.message || '加载商品列表失败')
    }
  } catch (error) {
    productOptions.value = []
    ElMessage.error(error?.response?.data?.message || error?.message || '加载商品列表失败')
  }
}

const openQuickCreateCustomer = () => {
  quickCreateCustomerState.open({
    customerCode: buildTempCode('AIC'),
    customerName: aiDraft.value?.customerName || '',
    contactPerson: '',
    phone: '',
    address: '',
    remark: 'AI识别草稿中快速新增客户'
  })
}

const openQuickCreateProduct = (row) => {
  quickCreateProductState.open({
    productCode: buildTempCode('AIP'),
    productName: row?.productName || '',
    specification: row?.specification || '',
    unit: row?.unit || '',
    category: 'AI识别新增',
    salePrice: Number(row?.unitPrice || 0),
    remark: 'AI识别草稿中快速新增商品'
  }, row)
}

const handleMatchedCustomerChange = (value) => {
  const selectedCustomer = customerOptions.value.find(item => Number(item.id) === Number(value))
  if (selectedCustomer && aiDraft.value) {
    aiDraft.value.customerName = selectedCustomer.customerName
    aiDraft.value.customerMatchStatus = 'manual_selected'
  } else if (aiDraft.value) {
    aiDraft.value.customerMatchStatus = 'unmatched'
  }
  markAiDraftDirty()
}

const handleMatchedProductChange = (row, value) => {
  const selectedProduct = productOptions.value.find(item => Number(item.id) === Number(value))
  if (selectedProduct) {
    row.productName = selectedProduct.productName
    row.specification = selectedProduct.specification || ''
    row.unit = selectedProduct.unit || ''
    row.matchStatus = 'manual_selected'
  } else {
    row.matchStatus = 'unmatched'
  }
  markAiDraftDirty()
}

const handleQuickCreateCustomer = async () => {
  if (!quickCreateCustomerForm.value.customerCode) {
    ElMessage.warning('请填写客户编码')
    return
  }
  if (!quickCreateCustomerForm.value.customerName) {
    ElMessage.warning('请填写客户名称')
    return
  }

  try {
    quickCreateCustomerState.loading.value = true
    const res = await addCustomer({
      customerCode: quickCreateCustomerForm.value.customerCode,
      customerName: quickCreateCustomerForm.value.customerName,
      contactPerson: quickCreateCustomerForm.value.contactPerson,
      phone: quickCreateCustomerForm.value.phone,
      address: quickCreateCustomerForm.value.address,
      remark: quickCreateCustomerForm.value.remark
    })
    if (res.data?.code === 1) {
      await loadCustomers()
      const created = customerOptions.value.find(item =>
        item.customerCode === quickCreateCustomerForm.value.customerCode
      )
      if (!created) {
        ElMessage.error('客户新增成功，但未能自动回填，请手动选择客户')
        quickCreateCustomerState.close()
        return
      }
      if (aiDraft.value) {
        aiDraft.value.matchedCustomerId = created.id
        aiDraft.value.customerName = created.customerName
        aiDraft.value.customerMatchStatus = 'manual_created'
        markAiDraftDirty()
      }
      quickCreateCustomerState.close()
      ElMessage.success('客户新增成功')
    } else {
      ElMessage.error(res.data?.message || '客户新增失败')
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '客户新增失败')
  } finally {
    quickCreateCustomerState.loading.value = false
  }
}

const handleQuickCreateProduct = async () => {
  if (!quickCreateProductForm.value.productCode) {
    ElMessage.warning('请填写商品编码')
    return
  }
  if (!quickCreateProductForm.value.productName) {
    ElMessage.warning('请填写商品名称')
    return
  }

  try {
    quickCreateProductState.loading.value = true
    const res = await addProduct({
      productCode: quickCreateProductForm.value.productCode,
      productName: quickCreateProductForm.value.productName,
      specification: quickCreateProductForm.value.specification,
      unit: quickCreateProductForm.value.unit,
      category: quickCreateProductForm.value.category,
      salePrice: quickCreateProductForm.value.salePrice,
      remark: quickCreateProductForm.value.remark
    })
    if (res.data?.code === 1) {
      await loadProducts()
      const created = productOptions.value.find(item =>
        item.productCode === quickCreateProductForm.value.productCode
      )
      if (!created) {
        ElMessage.error('商品新增成功，但未能自动回填，请手动选择商品')
        quickCreateProductState.close()
        return
      }
      if (quickCreateProductRow.value) {
        quickCreateProductRow.value.matchedProductId = created.id
        quickCreateProductRow.value.productName = created.productName
        quickCreateProductRow.value.specification = created.specification || ''
        quickCreateProductRow.value.unit = created.unit || ''
        quickCreateProductRow.value.matchStatus = 'manual_created'
        markAiDraftDirty()
      }
      quickCreateProductState.close()
      ElMessage.success('商品新增成功')
    } else {
      ElMessage.error(res.data?.message || '商品新增失败')
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '商品新增失败')
  } finally {
    quickCreateProductState.loading.value = false
  }
}

const beforeAiUpload = (file) => {
  const isAllowedType = ['image/jpeg', 'image/png'].includes(file.type)
  if (!isAllowedType) {
    ElMessage.error('当前只支持 jpg、jpeg、png 图片文件')
    return false
  }
  const isLt20M = file.size <= MAX_AI_FILE_SIZE
  if (!isLt20M) {
    ElMessage.error('文件不能超过 20MB')
    return false
  }
  return true
}

const handleAiFileChange = (file) => {
  if (!file || !file.raw) {
    resetAiDraftState()
    return
  }
  aiUploadFile.value = file.raw
  aiDraft.value = null
  aiManualReviewed.value = false
}

const handleAiFileRemove = () => {
  resetAiDraftState()
}

const handleAiRecognize = async () => {
  if (!aiUploadFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  if (aiUploadFile.value.size > MAX_AI_FILE_SIZE) {
    ElMessage.error('文件不能超过 20MB')
    return
  }

  try {
    aiRecognizing.value = true
    const formData = new FormData()
    formData.append('file', aiUploadFile.value)
    const res = await recognizeOutbound(formData)
    if (res.data?.code === 1) {
      aiDraft.value = normalizeAiDraft(res.data?.data)
      aiManualReviewed.value = false
      ElMessage.success('AI识别成功')
    } else {
      ElMessage.error(res.data?.message || 'AI识别失败')
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || 'AI识别失败')
  } finally {
    aiRecognizing.value = false
  }
}

const handleAiConfirm = async () => {
  if (aiConfirming.value) {
    return
  }
  if (!aiDraft.value) {
    ElMessage.warning('请先完成识别')
    return
  }
  if (!aiManualReviewed.value) {
    ElMessage.warning('请先人工校对识别结果后再生成出库单')
    return
  }
  if (!aiDraft.value.matchedCustomerId) {
    ElMessage.error('请先选择客户后再确认生成出库单')
    return
  }
  if (hasUnmatchedAiItems()) {
    ElMessage.error('存在未匹配商品，请先为每一行选择商品后再确认生成出库单')
    return
  }
  if (hasInvalidAiItems()) {
    ElMessage.error('AI识别明细中存在无效行号、数量、单价或金额，请修正后再确认生成出库单')
    return
  }

  try {
    aiConfirming.value = true
    const res = await confirmOutbound(buildAiConfirmPayload())

    if (res.data?.code === 1) {
      aiDialogVisible.value = false
      resetAiDraftState()
      const orderId = res.data?.data
      ElMessage.success(`AI确认成功，已生成出库单，ID=${orderId}`)
      await router.push(`/outbound/detail/${orderId}`)
    } else {
      ElMessage.error(res.data?.message || 'AI确认失败')
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || 'AI确认失败')
  } finally {
    aiConfirming.value = false
  }
}

const clearAiRecordQuery = async () => {
  if (route.path !== '/outbound/create' || !route.query.aiRecordId) {
    return
  }
  await router.replace({ path: '/outbound/create' })
}

const handleAiDialogClosed = () => {
  clearAiRecordQuery()
}

const loadAiDraftFromRecord = async (recordIdValue) => {
  const normalizedRecordId = Number(recordIdValue)
  if (!Number.isFinite(normalizedRecordId) || normalizedRecordId <= 0) {
    await clearAiRecordQuery()
    return
  }

  try {
    aiRecognizing.value = true
    const res = await getAiOutboundRecordDetail(normalizedRecordId)
    if (res.data?.code !== 1 || !res.data?.data) {
      ElMessage.error(res.data?.message || '加载AI识别记录失败')
      await clearAiRecordQuery()
      return
    }
    aiDraft.value = normalizeAiDraft(res.data.data)
    aiManualReviewed.value = false
    aiDialogVisible.value = true
    if (aiDraft.value?.confirmedOrderId) {
      ElMessage.warning(`该AI记录已生成正式出库单${aiDraft.value.confirmedOrderNo ? `（${aiDraft.value.confirmedOrderNo}）` : ''}`)
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '加载AI识别记录失败')
    await clearAiRecordQuery()
  } finally {
    aiRecognizing.value = false
  }
}

const openDialog = () => {
  aiDialogVisible.value = true
  if (!aiDraft.value) {
    aiManualReviewed.value = false
  }
}

defineExpose({
  openDialog
})

watch(
  () => route.query.aiRecordId,
  async (recordId) => {
    if (!recordId) {
      return
    }
    await loadAiDraftFromRecord(recordId)
  },
  { immediate: true }
)

onMounted(() => {
  loadCustomers()
  loadProducts()
})
</script>

<style scoped>
.base-form {
  margin-bottom: 16px;
}

.ai-upload-block {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.ai-result-block {
  margin-top: 12px;
}

.match-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
