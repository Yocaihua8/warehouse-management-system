<template>
  <el-dialog
    v-model="aiDialogVisible"
    title="AI智能识别导入"
    width="900px"
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

    <div v-if="aiDraft" class="ai-result-block" v-loading="aiConfirming">
      <el-alert
        title="识别完成，请确认草稿后生成正式入库单"
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
        <el-form-item label="供应商名称">
          <el-input v-model="aiDraft.supplierName" @input="markAiDraftDirty" />
        </el-form-item>

        <el-form-item label="匹配供应商">
          <div class="match-cell">
            <el-select
              v-model="aiDraft.matchedSupplierId"
              placeholder="请选择供应商"
              filterable
              remote
              reserve-keyword
              clearable
              :loading="supplierLoading"
              style="flex: 1"
              :remote-method="handleSupplierSearch"
              @change="handleMatchedSupplierChange"
            >
              <el-option
                v-for="item in supplierOptions"
                :key="item.id"
                :label="`${item.supplierCode} / ${item.supplierName}`"
                :value="item.id"
              />
            </el-select>

            <el-button
              v-if="!aiDraft.matchedSupplierId"
              type="primary"
              link
              @click="openQuickCreateSupplier"
            >
              新增供应商
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="单据备注">
          <el-input v-model="aiDraft.remark" @input="markAiDraftDirty" />
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

      <div class="table-toolbar ai-table-toolbar">
        <span class="section-title">AI识别明细</span>
        <el-button type="primary" @click="addAiItem">新增一行</el-button>
      </div>

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
                remote
                reserve-keyword
                clearable
                style="flex: 1"
                :remote-method="handleProductSearch"
                @change="(value) => handleAiMatchedProductChange(row, value)"
              >
                <el-option
                  v-for="item in productOptions"
                  :key="item.id"
                  :label="`${item.productName}${item.specification ? ' / ' + item.specification : ''}${item.unit ? ' / ' + item.unit : ''}`"
                  :value="item.id"
                />
              </el-select>

              <el-button type="primary" link @click="openQuickCreateProduct(row)">
                快速新建
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
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ $index }">
            <el-button
              type="danger"
              link
              @click="removeAiItem($index)"
              :disabled="(aiDraft.itemList || []).length === 1"
            >
              删除
            </el-button>
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
          确认生成入库单
        </el-button>
      </div>
    </template>
  </el-dialog>

  <QuickCreateDialog
    v-model:visible="quickCreateSupplierVisible"
    title="快速新建供应商"
    :loading="quickCreateSupplierLoading"
    confirm-text="保存供应商"
    @confirm="handleQuickCreateSupplier"
  >
    <el-form label-width="90px">
      <el-form-item label="供应商编码">
        <el-input v-model="quickCreateSupplierForm.supplierCode" />
      </el-form-item>
      <el-form-item label="供应商名称">
        <el-input v-model="quickCreateSupplierForm.supplierName" />
      </el-form-item>
      <el-form-item label="联系人">
        <el-input v-model="quickCreateSupplierForm.contactPerson" />
      </el-form-item>
      <el-form-item label="联系电话">
        <el-input v-model="quickCreateSupplierForm.phone" />
      </el-form-item>
      <el-form-item label="地址">
        <el-input v-model="quickCreateSupplierForm.address" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="quickCreateSupplierForm.remark" type="textarea" :rows="2" />
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
import { addProduct, getProductList } from '../../api/product'
import { addSupplier, getSupplierList } from '../../api/supplier'
import { confirmInbound, getAiInboundRecordDetail, recognizeInbound } from '../../api/ai'
import { getInboundOrderDetail } from '../../api/inbound'
import { useProductSearch } from '../../composables/useProductSearch'
import { parsePageData } from '../../utils/orderHelper'
import QuickCreateDialog from './QuickCreateDialog.vue'

const route = useRoute()
const router = useRouter()

const MAX_AI_FILE_SIZE = 20 * 1024 * 1024

const aiDialogVisible = ref(false)
const aiRecognizing = ref(false)
const aiConfirming = ref(false)
const aiUploadFile = ref(null)
const aiDraft = ref(null)
const aiManualReviewed = ref(false)

const supplierOptions = ref([])
const supplierLoading = ref(false)

const quickCreateSupplierVisible = ref(false)
const quickCreateSupplierLoading = ref(false)
const quickCreateSupplierForm = ref({
  supplierCode: '',
  supplierName: '',
  contactPerson: '',
  phone: '',
  address: '',
  remark: ''
})

const quickCreateProductVisible = ref(false)
const quickCreateProductLoading = ref(false)
const quickCreateProductRow = ref(null)
const quickCreateProductForm = ref({
  productCode: '',
  productName: '',
  specification: '',
  unit: '',
  category: '',
  salePrice: 0,
  remark: ''
})

const {
  productOptions,
  handleProductSearch,
  upsertProductOption,
  loadProducts
} = useProductSearch({
  fetchProductList: getProductList,
  onError: (message) => ElMessage.error(message)
})

const buildTempProductCode = () => `AI${Date.now()}`
const buildTempSupplierCode = () => `AIS${Date.now()}`

const upsertSupplierOption = (supplier) => {
  if (!supplier || supplier.id == null) {
    return
  }
  const exists = supplierOptions.value.some(item => Number(item.id) === Number(supplier.id))
  if (!exists) {
    supplierOptions.value = [supplier, ...supplierOptions.value]
  }
}

const normalizeInboundAiItem = (item = {}, index = 0) => ({
  lineNo: Number(item.lineNo) || index + 1,
  productName: item.productName || '',
  specification: item.specification || '',
  unit: item.unit || '',
  quantity: Number(item.quantity) || 1,
  unitPrice: Number(item.unitPrice ?? 0),
  amount: Number(item.amount ?? ((Number(item.quantity) || 0) * Number(item.unitPrice ?? 0))),
  matchedProductId: item.matchedProductId ?? null,
  matchStatus: item.matchStatus || 'unmatched',
  remark: item.remark || ''
})

const normalizeInboundAiDraft = (draft) => {
  if (!draft) {
    return null
  }
  const normalizedItemList = Array.isArray(draft.itemList)
    ? draft.itemList.map((item, index) => normalizeInboundAiItem(item, index))
    : []

  return {
    ...draft,
    supplierName: draft.supplierName || '',
    matchedSupplierId: draft.matchedSupplierId ?? null,
    supplierMatchStatus: draft.supplierMatchStatus || 'unmatched',
    rawText: draft.rawText || '',
    remark: draft.remark || 'AI识别确认生成入库单',
    itemList: normalizedItemList.length > 0 ? normalizedItemList : [normalizeInboundAiItem({}, 0)]
  }
}

const markAiDraftDirty = () => {
  if (!aiDraft.value) {
    return
  }
  aiManualReviewed.value = false
}

const formatWarningText = (warnings, warningsJson) => {
  if (Array.isArray(warnings) && warnings.length > 0) {
    return warnings.join('；')
  }
  if (!warningsJson) {
    return ''
  }
  try {
    const parsed = JSON.parse(warningsJson)
    return Array.isArray(parsed) ? parsed.join('；') : warningsJson
  } catch (error) {
    return warningsJson
  }
}

const getAiWarningText = () => formatWarningText(aiDraft.value?.warnings, aiDraft.value?.warningsJson)

const loadSupplierOptions = async () => {
  try {
    supplierLoading.value = true
    const res = await getSupplierList({ pageNum: 1, pageSize: 200 })
    if (res.data?.code === 1) {
      supplierOptions.value = parsePageData(res.data?.data).list
    } else {
      supplierOptions.value = []
      ElMessage.error(res.data?.message || '加载供应商列表失败')
    }
  } catch (error) {
    supplierOptions.value = []
    ElMessage.error(error?.response?.data?.message || error?.message || '加载供应商列表失败')
  } finally {
    supplierLoading.value = false
  }
}

const handleSupplierSearch = async (keyword) => {
  try {
    supplierLoading.value = true
    const res = await getSupplierList({
      supplierName: (keyword || '').trim() || undefined,
      pageNum: 1,
      pageSize: 200
    })
    if (res.data?.code === 1) {
      supplierOptions.value = parsePageData(res.data?.data).list
    } else {
      supplierOptions.value = []
      ElMessage.error(res.data?.message || '加载供应商列表失败')
    }
  } catch (error) {
    supplierOptions.value = []
    ElMessage.error(error?.response?.data?.message || error?.message || '加载供应商列表失败')
  } finally {
    supplierLoading.value = false
  }
}

const handleMatchedSupplierChange = (supplierId) => {
  const selectedSupplier = supplierOptions.value.find(item => Number(item.id) === Number(supplierId))
  if (selectedSupplier && aiDraft.value) {
    aiDraft.value.supplierName = selectedSupplier.supplierName
    aiDraft.value.supplierMatchStatus = 'manual_selected'
  } else if (aiDraft.value) {
    aiDraft.value.supplierMatchStatus = 'unmatched'
  }
  markAiDraftDirty()
}

const handleAiMatchedProductChange = (row, productId) => {
  const selectedProduct = productOptions.value.find(item => Number(item.id) === Number(productId))
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

const addAiItem = () => {
  if (!aiDraft.value) {
    return
  }
  aiDraft.value.itemList.push(normalizeInboundAiItem({}, aiDraft.value.itemList.length))
  markAiDraftDirty()
}

const removeAiItem = (index) => {
  if (!aiDraft.value || !Array.isArray(aiDraft.value.itemList)) {
    return
  }
  if (aiDraft.value.itemList.length === 1) {
    ElMessage.warning('至少保留一条AI识别明细')
    return
  }
  aiDraft.value.itemList.splice(index, 1)
  aiDraft.value.itemList.forEach((item, itemIndex) => {
    item.lineNo = itemIndex + 1
  })
  markAiDraftDirty()
}

const hasUnmatchedAiItems = () => (aiDraft.value?.itemList || []).some(item => !item.matchedProductId)

const hasInvalidAiItems = () => {
  return (aiDraft.value?.itemList || []).some(item => {
    const lineNo = Number(item.lineNo)
    const quantity = Number(item.quantity)
    const unitPrice = Number(item.unitPrice)
    const amount = Number(item.amount)
    return !Number.isFinite(lineNo) ||
      lineNo <= 0 ||
      !Number.isFinite(quantity) ||
      quantity <= 0 ||
      !Number.isFinite(unitPrice) ||
      unitPrice < 0 ||
      !Number.isFinite(amount) ||
      amount < 0
  })
}

const findCreatedSupplierByCode = async (supplierCode) => {
  const res = await getSupplierList({ supplierCode, pageNum: 1, pageSize: 10 })
  if (res.data?.code !== 1) {
    throw new Error(res.data?.message || '查询新增供应商失败')
  }
  return parsePageData(res.data?.data).list.find(item => item.supplierCode === supplierCode) || null
}

const findCreatedProductByCode = async (productCode) => {
  const res = await getProductList({ productCode, pageNum: 1, pageSize: 10 })
  if (res.data?.code !== 1) {
    throw new Error(res.data?.message || '查询新增商品失败')
  }
  return parsePageData(res.data?.data).list.find(item => item.productCode === productCode) || null
}

const openQuickCreateSupplier = () => {
  quickCreateSupplierForm.value = {
    supplierCode: buildTempSupplierCode(),
    supplierName: aiDraft.value?.supplierName || '',
    contactPerson: '',
    phone: '',
    address: '',
    remark: 'AI识别草稿中快速新建供应商'
  }
  quickCreateSupplierVisible.value = true
}

const openQuickCreateProduct = (row) => {
  quickCreateProductRow.value = row
  quickCreateProductForm.value = {
    productCode: buildTempProductCode(),
    productName: row.productName || '',
    specification: row.specification || '',
    unit: row.unit || '',
    category: 'AI识别新增',
    salePrice: Number(row.unitPrice || 0),
    remark: 'AI识别草稿中快速新建'
  }
  quickCreateProductVisible.value = true
}

const handleQuickCreateSupplier = async () => {
  if (!quickCreateSupplierForm.value.supplierCode) {
    ElMessage.warning('请填写供应商编码')
    return
  }
  if (!quickCreateSupplierForm.value.supplierName) {
    ElMessage.warning('请填写供应商名称')
    return
  }

  try {
    quickCreateSupplierLoading.value = true
    const res = await addSupplier({
      supplierCode: quickCreateSupplierForm.value.supplierCode,
      supplierName: quickCreateSupplierForm.value.supplierName,
      contactPerson: quickCreateSupplierForm.value.contactPerson,
      phone: quickCreateSupplierForm.value.phone,
      address: quickCreateSupplierForm.value.address,
      remark: quickCreateSupplierForm.value.remark
    })
    if (res.data?.code === 1) {
      const created = await findCreatedSupplierByCode(quickCreateSupplierForm.value.supplierCode)
      if (!created) {
        ElMessage.error('供应商新增成功，但未能自动回填，请手动选择供应商')
        quickCreateSupplierVisible.value = false
        return
      }
      upsertSupplierOption(created)
      if (aiDraft.value) {
        aiDraft.value.matchedSupplierId = created.id
        aiDraft.value.supplierName = created.supplierName
        aiDraft.value.supplierMatchStatus = 'manual_created'
        markAiDraftDirty()
      }
      quickCreateSupplierVisible.value = false
      ElMessage.success('供应商新增成功')
    } else {
      ElMessage.error(res.data?.message || '供应商新增失败')
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '供应商新增失败')
  } finally {
    quickCreateSupplierLoading.value = false
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
  if (!quickCreateProductForm.value.unit) {
    ElMessage.warning('请填写单位')
    return
  }

  try {
    quickCreateProductLoading.value = true
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
      const created = await findCreatedProductByCode(quickCreateProductForm.value.productCode)
      if (created && quickCreateProductRow.value) {
        upsertProductOption(created)
        quickCreateProductRow.value.matchedProductId = created.id
        quickCreateProductRow.value.productName = created.productName
        quickCreateProductRow.value.specification = created.specification || ''
        quickCreateProductRow.value.unit = created.unit || ''
        quickCreateProductRow.value.matchStatus = 'manual_created'
        markAiDraftDirty()
      }
      quickCreateProductVisible.value = false
      quickCreateProductRow.value = null
      ElMessage.success('商品新建成功')
    } else {
      ElMessage.error(res.data?.message || '商品新建失败')
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '商品新建失败')
  } finally {
    quickCreateProductLoading.value = false
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
    aiUploadFile.value = null
    aiDraft.value = null
    aiManualReviewed.value = false
    return
  }
  aiUploadFile.value = file.raw
  aiDraft.value = null
  aiManualReviewed.value = false
}

const handleAiFileRemove = () => {
  aiUploadFile.value = null
  aiDraft.value = null
  aiManualReviewed.value = false
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
    const res = await recognizeInbound(formData)
    if (res.data?.code === 1) {
      aiDraft.value = normalizeInboundAiDraft(res.data?.data)
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

const buildAiConfirmPayload = () => ({
  recordId: aiDraft.value?.recordId,
  supplierId: aiDraft.value?.matchedSupplierId != null ? Number(aiDraft.value.matchedSupplierId) : null,
  supplierName: aiDraft.value?.supplierName || '',
  rawText: aiDraft.value?.rawText || '',
  remark: aiDraft.value?.remark || 'AI识别确认生成入库单',
  itemList: (aiDraft.value?.itemList || []).map(item => ({
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

const handleAiConfirmError = async (message) => {
  const errorMessage = message || 'AI确认失败'
  if (!errorMessage.includes('已确认生成入库单') && !errorMessage.includes('已生成正式入库单')) {
    ElMessage.error(errorMessage)
    return
  }

  try {
    const recordId = aiDraft.value?.recordId
    if (recordId) {
      const res = await getAiInboundRecordDetail(recordId)
      const latestRecord = res.data?.code === 1 ? res.data?.data : null
      const confirmedOrderId = latestRecord?.confirmedOrderId
      const confirmedOrderNo = latestRecord?.confirmedOrderNo
      if (confirmedOrderId) {
        ElMessage.warning(`该AI记录已经生成正式入库单${confirmedOrderNo ? `（${confirmedOrderNo}）` : ''}，不能重复确认，正在跳转`)
        aiDialogVisible.value = false
        await router.push(`/inbound/detail/${confirmedOrderId}`)
        return
      }
    }
  } catch (error) {
    console.error('查询最新AI记录状态失败：', error)
  }
  ElMessage.warning(errorMessage)
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
    ElMessage.warning('请先人工校对识别结果后再生成入库单')
    return
  }
  if (!aiDraft.value.supplierName || !aiDraft.value.supplierName.trim()) {
    ElMessage.error('请先确认供应商名称后再生成入库单')
    return
  }
  if (hasUnmatchedAiItems()) {
    ElMessage.error('存在未匹配商品，请先为每一行选择商品后再确认生成入库单')
    return
  }
  if (hasInvalidAiItems()) {
    ElMessage.error('AI识别明细中存在无效行号、数量、单价或金额，请修正后再确认生成入库单')
    return
  }

  try {
    aiConfirming.value = true
    const res = await confirmInbound(buildAiConfirmPayload())
    if (res.data?.code === 1) {
      aiDialogVisible.value = false
      aiDraft.value = null
      aiUploadFile.value = null
      aiManualReviewed.value = false

      const orderId = res.data?.data
      let orderNo = ''
      try {
        const detailRes = await getInboundOrderDetail(orderId)
        if (detailRes.data?.code === 1) {
          orderNo = detailRes.data?.data?.orderNo || ''
        }
      } catch (error) {
        console.error('获取正式入库单号失败：', error)
      }
      ElMessage.success(orderNo
        ? `AI确认成功，已生成正式入库单：${orderNo}，正在跳转详情`
        : `AI确认成功，已生成正式入库单，ID=${orderId}`)
      await router.push(`/inbound/detail/${orderId}`)
    } else {
      await handleAiConfirmError(res.data?.message || 'AI确认失败')
    }
  } catch (error) {
    await handleAiConfirmError(error?.response?.data?.message || error?.message || 'AI确认失败')
  } finally {
    aiConfirming.value = false
  }
}

const loadAiDraftFromRecord = async (recordId) => {
  const normalizedRecordId = Number(recordId)
  if (!Number.isFinite(normalizedRecordId) || normalizedRecordId <= 0) {
    return
  }
  try {
    aiRecognizing.value = true
    aiDialogVisible.value = false
    aiUploadFile.value = null
    const res = await getAiInboundRecordDetail(normalizedRecordId)
    if (res.data?.code !== 1) {
      ElMessage.error(res.data?.message || '加载AI识别记录失败')
      return
    }
    const draft = normalizeInboundAiDraft(res.data?.data)
    if (draft?.confirmedOrderId) {
      ElMessage.warning(`该AI记录已生成正式入库单${draft.confirmedOrderNo ? `（${draft.confirmedOrderNo}）` : ''}，不能重复确认`)
      await router.replace(`/inbound/detail/${draft.confirmedOrderId}`)
      return
    }
    aiDraft.value = draft
    aiManualReviewed.value = false
    aiDialogVisible.value = true

    if (draft?.matchedSupplierId) {
      upsertSupplierOption({
        id: draft.matchedSupplierId,
        supplierCode: `ID:${draft.matchedSupplierId}`,
        supplierName: draft.supplierName || `供应商${draft.matchedSupplierId}`
      })
    }

    ;(draft?.itemList || []).forEach((item) => {
      if (item.matchedProductId) {
        upsertProductOption({
          id: item.matchedProductId,
          productName: item.productName || `商品${item.matchedProductId}`,
          specification: item.specification || '',
          unit: item.unit || ''
        })
      }
    })
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '加载AI识别记录失败')
  } finally {
    aiRecognizing.value = false
  }
}

const clearAiRecordQuery = async () => {
  if (route.path !== '/inbound/create' || !route.query.aiRecordId) {
    return
  }
  await router.replace({ path: '/inbound/create' })
}

const handleAiDialogClosed = () => {
  clearAiRecordQuery()
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
  loadProducts()
  loadSupplierOptions()
})
</script>

<style scoped>
.base-form {
  margin-bottom: 24px;
}

.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
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

.ai-table-toolbar {
  margin-bottom: 12px;
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
