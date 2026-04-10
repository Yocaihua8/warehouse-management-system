<template>
  <div class="page-container">
    <el-card class="page-card" shadow="never">
      <template #header>
        <span class="page-title">{{ pageTitle }}</span>
      </template>

      <el-form :model="form" label-width="100px" class="section section-header">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="单据编号">
              <el-input v-model="form.orderNo" disabled placeholder="保存草稿后自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单据日期">
              <el-date-picker
                v-model="form.orderDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
                :disabled="!editable"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="来源类型">
              <el-input :model-value="form.sourceType === 'AI' ? 'AI' : 'MANUAL'" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商名称">
              <el-input v-model="form.supplierName" placeholder="请输入供应商名称" :disabled="!editable" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="请输入备注" :disabled="!editable" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="section section-items">
        <OrderItemTable
          :items="form.itemList"
          order-type="inbound"
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
      </div>
      <div class="section section-actions">
        <el-button :disabled="!canClear" @click="handleClear">清空 (Alt+C)</el-button>
        <el-button type="success" :disabled="!canSaveDraft" :loading="submitting" @click="handleSubmit">
          {{ currentDraftId ? '更新草稿 (Ctrl+S)' : '保存草稿 (Ctrl+S)' }}
        </el-button>
        <el-button :disabled="!canSaveAndNew" :loading="submitting" @click="handleSaveAndNew">保存并新建 (Ctrl+Shift+S)</el-button>
        <el-button type="warning" plain :disabled="!canSubmitConfirm" @click="handleSubmitConfirm">提交确认</el-button>
        <el-button type="primary" plain :disabled="!canPrintPreview" @click="openPrintPreview">打印</el-button>
        <el-button plain :disabled="!canPrintPreview" @click="openPrintPreview">预览 (Alt+P)</el-button>
        <el-button type="primary" plain :disabled="!canAiImport" @click="openAiDialog">智能识别导入</el-button>
      </div>
    </el-card>

    <AiRecognitionDialog ref="aiDialogRef" />
  </div>
</template>

<script setup>
import { useInboundCreatePage } from '../../composables/useInboundCreatePage'
import OrderItemTable from '../../components/order/OrderItemTable.vue'
import AiRecognitionDialog from '../../components/order/AiRecognitionDialog.vue'

const {
  aiDialogRef,
  form,
  editable,
  pageTitle,
  submitting,
  currentDraftId,
  productOptions,
  productLoading,
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
  handleSaveAndNew,
  handleSubmitConfirm,
  openPrintPreview,
  canSaveDraft,
  canSaveAndNew,
  canClear,
  canAiImport,
  canSubmitConfirm,
  canPrintPreview
} = useInboundCreatePage()
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

.section {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 14px;
}

.section-header {
  margin-bottom: 16px;
}

.section-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: flex-end;
}

</style>
