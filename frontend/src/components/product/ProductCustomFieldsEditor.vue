<template>
  <div class="custom-fields-editor">
    <div class="editor-toolbar">
      <span class="editor-tip">键值对会自动序列化为 JSON 保存，留空行不会提交。</span>
      <el-button link type="primary" @click="handleAdd">+ 新增字段</el-button>
    </div>

    <div
      v-for="(row, index) in rows"
      :key="`${index}-${row.key}`"
      class="editor-row"
    >
      <el-input
        :model-value="row.key"
        placeholder="字段名，如 品牌"
        @update:model-value="handleChange(index, 'key', $event)"
      />
      <el-input
        :model-value="row.value"
        placeholder="字段值，如 A牌"
        @update:model-value="handleChange(index, 'value', $event)"
      />
      <el-button
        link
        type="danger"
        @click="handleRemove(index)"
      >
        删除
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { createEmptyCustomField, ensureCustomFieldRows } from '../../utils/productCustomFields'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [createEmptyCustomField()]
  }
})

const emit = defineEmits(['update:modelValue'])

const rows = computed(() => ensureCustomFieldRows(props.modelValue))

const updateRows = (nextRows) => {
  emit('update:modelValue', ensureCustomFieldRows(nextRows))
}

const handleAdd = () => {
  updateRows([...rows.value, createEmptyCustomField()])
}

const handleRemove = (index) => {
  const nextRows = rows.value.filter((_, rowIndex) => rowIndex !== index)
  updateRows(nextRows.length > 0 ? nextRows : [createEmptyCustomField()])
}

const handleChange = (index, field, value) => {
  const nextRows = rows.value.map((row, rowIndex) => {
    if (rowIndex !== index) {
      return { ...row }
    }
    return {
      ...row,
      [field]: value
    }
  })
  updateRows(nextRows)
}
</script>

<style scoped>
.custom-fields-editor {
  width: 100%;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.editor-tip {
  font-size: 13px;
  color: #6b7280;
}

.editor-row {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(220px, 1.4fr) auto;
  gap: 12px;
  align-items: center;
}

.editor-row + .editor-row {
  margin-top: 10px;
}

@media (max-width: 900px) {
  .editor-row {
    grid-template-columns: 1fr;
  }

  .editor-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
