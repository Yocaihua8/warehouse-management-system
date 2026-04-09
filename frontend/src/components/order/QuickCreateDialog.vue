<template>
  <el-dialog v-model="innerVisible" :title="title" :width="width">
    <slot />
    <template #footer>
      <el-button @click="innerVisible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="loading"
        @click="emit('confirm')"
      >
        {{ confirmText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: ''
  },
  width: {
    type: String,
    default: '500px'
  },
  loading: {
    type: Boolean,
    default: false
  },
  confirmText: {
    type: String,
    default: '保存'
  }
})

const emit = defineEmits(['update:visible', 'confirm'])

const innerVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})
</script>
