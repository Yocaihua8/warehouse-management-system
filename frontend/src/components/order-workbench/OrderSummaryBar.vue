<template>
  <footer class="order-summary-bar">
    <div class="summary-main">
      <div class="summary-values">
        <div class="summary-item">
          <span class="summary-label">合计数量</span>
          <strong class="summary-value">{{ summary.totalQuantity }}</strong>
        </div>
        <div class="summary-item">
          <span class="summary-label">合计金额</span>
          <strong class="summary-value">¥{{ summary.totalAmount }}</strong>
        </div>
      </div>
      <div class="summary-remark">
        <span class="summary-label">备注</span>
        <span class="summary-remark-text">{{ header?.remark || '—' }}</span>
      </div>
    </div>

    <div class="summary-actions">
      <div class="summary-aux-actions">
        <slot name="aux-actions" />
      </div>
      <div class="summary-primary-actions">
        <el-button :disabled="cancelDisabled" @click="$emit('cancel')">取消</el-button>
        <el-button
          type="success"
          :disabled="saveDisabled"
          :loading="saving"
          @click="$emit('save-draft')"
        >
          {{ saveLabel }}
        </el-button>
        <el-button
          type="primary"
          :disabled="submitDisabled"
          @click="$emit('submit-order')"
        >
          {{ submitLabel }}
        </el-button>
      </div>
    </div>
  </footer>
</template>

<script setup>
defineProps({
  summary: {
    type: Object,
    required: true
  },
  header: {
    type: Object,
    default: () => ({})
  },
  saving: {
    type: Boolean,
    default: false
  },
  saveDisabled: {
    type: Boolean,
    default: false
  },
  submitDisabled: {
    type: Boolean,
    default: false
  },
  cancelDisabled: {
    type: Boolean,
    default: false
  },
  saveLabel: {
    type: String,
    default: '保存草稿'
  },
  submitLabel: {
    type: String,
    default: '提交确认'
  }
})

defineEmits(['save-draft', 'submit-order', 'cancel'])
</script>

<style scoped>
.order-summary-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid #e8edf4;
  border-radius: 10px;
  padding: 16px 18px;
  background: #fff;
}

.summary-main {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.summary-values {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.summary-item,
.summary-remark {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-label {
  color: #6b7280;
  font-size: 14px;
}

.summary-value {
  font-size: 18px;
  color: #111827;
}

.summary-remark-text {
  color: #111827;
}

.summary-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  flex-wrap: wrap;
}

.summary-aux-actions,
.summary-primary-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 1200px) {
  .order-summary-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .summary-actions {
    justify-content: space-between;
  }
}
</style>
