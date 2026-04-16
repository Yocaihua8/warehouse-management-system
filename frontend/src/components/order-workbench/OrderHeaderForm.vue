<template>
  <section class="order-header-form">
    <div class="header-meta">
      <div class="meta-left">
        <div class="meta-title">{{ documentTypeLabel }}</div>
        <div class="meta-subtitle">
          <span>来源类型：</span>
          <strong>{{ sourceTypeLabel }}</strong>
        </div>
      </div>
      <div class="meta-right">
        <div class="meta-line">
          <span class="meta-label">单据编号</span>
          <strong class="meta-value">{{ form.orderNo || '待生成' }}</strong>
        </div>
        <div class="meta-line">
          <span class="meta-label">单据日期</span>
          <el-date-picker
            v-model="form.orderDate"
            type="date"
            value-format="YYYY-MM-DD"
            :disabled="!editable"
            class="meta-date-picker"
          />
        </div>
      </div>
    </div>

    <el-form :model="form" label-width="72px" class="header-form">
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12" :lg="8">
          <el-form-item label="单据类型">
            <el-input :model-value="documentTypeLabel" disabled />
          </el-form-item>
        </el-col>

        <el-col v-if="orderType === 'inbound'" :xs="24" :sm="12" :lg="8">
          <el-form-item label="供应商">
            <el-input
              v-model="form.supplierName"
              placeholder="请输入供应商名称"
              :disabled="!editable"
            />
          </el-form-item>
        </el-col>

        <el-col v-else :xs="24" :sm="12" :lg="8">
          <el-form-item label="客户">
            <el-select
              v-model="form.customerId"
              placeholder="请选择客户"
              filterable
              :disabled="!editable"
              style="width: 100%"
            >
              <el-option
                v-for="item in customerOptions"
                :key="item.id"
                :label="item.customerName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :xs="24" :sm="12" :lg="8">
          <el-form-item label="仓库">
            <el-input :model-value="warehouseName" disabled />
          </el-form-item>
        </el-col>

        <el-col :xs="24" :sm="12" :lg="8">
          <el-form-item label="经办人">
            <el-input :model-value="operatorName" disabled />
          </el-form-item>
        </el-col>

        <el-col :xs="24" :sm="12" :lg="16">
          <el-form-item label="备注">
            <el-input
              v-model="form.remark"
              placeholder="请输入备注"
              :disabled="!editable"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  orderType: {
    type: String,
    default: 'inbound'
  },
  form: {
    type: Object,
    required: true
  },
  editable: {
    type: Boolean,
    default: true
  },
  customerOptions: {
    type: Array,
    default: () => []
  },
  warehouseName: {
    type: String,
    default: '总仓库'
  },
  operatorName: {
    type: String,
    default: ''
  }
})

const documentTypeLabel = computed(() => {
  return props.orderType === 'outbound' ? '出库单' : '入库单'
})

const sourceTypeLabel = computed(() => {
  return props.form?.sourceType === 'AI' ? 'AI' : 'MANUAL'
})
</script>

<style scoped>
.order-header-form {
  border: 1px solid #e8edf4;
  border-radius: 10px;
  padding: 18px 18px 8px;
  background: #fff;
}

.header-meta {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.meta-left,
.meta-right {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.meta-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
}

.meta-subtitle,
.meta-line {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #4b5563;
}

.meta-label {
  min-width: 60px;
  color: #6b7280;
}

.meta-value {
  font-size: 16px;
  color: #111827;
}

.meta-date-picker {
  width: 180px;
}

.header-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

@media (max-width: 992px) {
  .header-meta {
    flex-direction: column;
  }

  .meta-date-picker {
    width: 100%;
  }
}
</style>
