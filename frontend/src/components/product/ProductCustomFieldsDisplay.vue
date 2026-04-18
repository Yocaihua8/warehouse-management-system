<template>
  <span v-if="compact">{{ summaryText }}</span>
  <div v-else-if="entries.length" class="custom-fields-display">
    <div
      v-for="entry in entries"
      :key="entry.key"
      class="custom-field-item"
    >
      <span class="field-key">{{ entry.key }}</span>
      <span class="field-separator">：</span>
      <span class="field-value">{{ entry.value || '-' }}</span>
    </div>
  </div>
  <span v-else>-</span>
</template>

<script setup>
import { computed } from 'vue'
import { getCustomFieldEntries, summarizeCustomFields } from '../../utils/productCustomFields'

const props = defineProps({
  value: {
    type: String,
    default: ''
  },
  compact: {
    type: Boolean,
    default: false
  }
})

const entries = computed(() => getCustomFieldEntries(props.value))
const summaryText = computed(() => summarizeCustomFields(props.value))
</script>

<style scoped>
.custom-fields-display {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 220px;
}

.custom-field-item {
  line-height: 1.5;
}

.field-key {
  font-weight: 600;
  color: #111827;
}

.field-value {
  color: #4b5563;
  word-break: break-all;
}
</style>
