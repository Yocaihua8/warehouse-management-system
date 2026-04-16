import { ref } from 'vue'

export function useQuickCreate() {
  const buildTempCode = (prefix = 'TMP') => `${prefix}${Date.now()}`

  const createQuickCreateState = (initialForm = {}) => {
    const visible = ref(false)
    const loading = ref(false)
    const form = ref({ ...initialForm })
    const targetRow = ref(null)

    const open = (nextForm = {}, row = null) => {
      form.value = { ...initialForm, ...nextForm }
      targetRow.value = row
      visible.value = true
    }

    const close = () => {
      visible.value = false
      targetRow.value = null
    }

    const reset = () => {
      form.value = { ...initialForm }
      targetRow.value = null
    }

    return {
      visible,
      loading,
      form,
      targetRow,
      open,
      close,
      reset
    }
  }

  return {
    buildTempCode,
    createQuickCreateState
  }
}
