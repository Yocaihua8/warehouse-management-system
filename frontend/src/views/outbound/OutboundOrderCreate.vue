<template>
  <div class="page-container">
    <div class="order-workbench">
      <header class="page-header">
        <h1 class="page-title">{{ pageTitle }}</h1>
      </header>

      <OrderHeaderForm
        order-type="outbound"
        :form="form"
        :editable="editable"
        :customer-options="customerOptions"
        :warehouse-name="warehouseName"
        :operator-name="operatorName"
      />

      <OrderDetailTable
        ref="detailTableRef"
        order-type="outbound"
        :items="form.itemList"
        :editable="editable"
        :product-options="productOptions"
        :product-loading="productLoading"
        :calc-amount="calcAmount"
        @row-add="addItem"
        @row-insert="insertItem"
        @row-delete="removeItem"
        @row-field-change="({ index, field, value }) => updateRowField(index, field, value)"
        @product-search="handleProductSearch"
        @product-selected="onProductChange"
      />

      <OrderSummaryBar
        :summary="summary"
        :header="form"
        :saving="submitting"
        :save-disabled="!canSaveDraft"
        :submit-disabled="!canSubmitConfirm"
        :save-label="currentDraftId ? '更新草稿' : '保存草稿'"
        submit-label="提交确认"
        @save-draft="handleSubmit"
        @submit-order="handleSubmitConfirm"
        @cancel="handleCancel"
      >
        <template #aux-actions>
          <el-button :disabled="!canClear" @click="handleClear">清空</el-button>
          <el-button :disabled="!canSaveAndNew" :loading="submitting" @click="handleSaveAndNew">保存并新建</el-button>
          <el-button type="primary" plain :disabled="!canAiImport" @click="openAiDialog">智能识别导入</el-button>
          <el-button type="primary" plain :disabled="!canPrintPreview" @click="openPrintPreview">打印</el-button>
          <el-button plain :disabled="!canPrintPreview" @click="openPrintPreview">预览</el-button>
        </template>
      </OrderSummaryBar>
    </div>

    <AiOutboundRecognitionDialog ref="aiDialogRef" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import OrderDetailTable from '../../components/order-workbench/OrderDetailTable.vue'
import OrderHeaderForm from '../../components/order-workbench/OrderHeaderForm.vue'
import OrderSummaryBar from '../../components/order-workbench/OrderSummaryBar.vue'
import AiOutboundRecognitionDialog from '../../components/order/AiOutboundRecognitionDialog.vue'
import { useOutboundCreatePage } from '../../composables/useOutboundCreatePage'
import { getNickname, getUsername } from '../../utils/auth'

const router = useRouter()
const detailTableRef = ref(null)

const {
  aiDialogRef,
  form,
  editable,
  pageTitle,
  submitting,
  currentDraftId,
  customerOptions,
  productOptions,
  productLoading,
  summary,
  calcAmount,
  addItem,
  insertItem,
  removeItem,
  updateRowField,
  handleProductSearch,
  onProductChange,
  openAiDialog,
  handleClear,
  handleSubmit,
  handleSaveAndNew: saveAndCreateNext,
  handleSubmitConfirm,
  openPrintPreview,
  canSaveDraft,
  canSaveAndNew,
  canClear,
  canAiImport,
  canSubmitConfirm,
  canPrintPreview
} = useOutboundCreatePage()

const warehouseName = '总仓库'
const operatorName = computed(() => getNickname() || getUsername() || '当前用户')

const handleSaveAndNew = async () => {
  const saved = await saveAndCreateNext()
  if (!saved) {
    return
  }
  await detailTableRef.value?.focusFirstEditableCell?.()
}

const handleCancel = () => {
  router.push('/outbound/list')
}
</script>

<style scoped>
.page-container {
  padding: 24px;
}

.order-workbench {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #111827;
}
</style>
