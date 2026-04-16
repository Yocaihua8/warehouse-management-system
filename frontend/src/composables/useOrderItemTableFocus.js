import { nextTick, ref, unref, watch } from 'vue'

const FOCUS_FIELDS = ['productSelect', 'productCode', 'productName', 'specification', 'unit', 'quantity', 'unitPrice', 'remark']

export function useOrderItemTableFocus({ editable, resolvedItems, onAppendRow }) {
  const cellRefMap = new Map()
  const activeRowIndex = ref(-1)

  const isEditable = () => Boolean(unref(editable))
  const getItems = () => unref(resolvedItems) || []

  const buildCellKey = (rowIndex, field) => `${rowIndex}:${field}`

  const setCellRef = (rowIndex, field) => (el) => {
    const key = buildCellKey(rowIndex, field)
    if (el) {
      cellRefMap.set(key, el)
      return
    }
    cellRefMap.delete(key)
  }

  const resolveFocusableElement = (container) => {
    if (!container) {
      return null
    }
    const textInput = container.querySelector('input:not([disabled]):not([tabindex="-1"]), textarea:not([disabled]):not([tabindex="-1"])')
    if (textInput) {
      return textInput
    }
    return container.querySelector('button:not([disabled]):not([tabindex="-1"]), [tabindex]:not([tabindex="-1"])')
  }

  const setActiveRow = (rowIndex) => {
    if (!isEditable() || rowIndex < 0) {
      return
    }
    activeRowIndex.value = rowIndex
  }

  const resolveRowClassName = ({ rowIndex }) => {
    if (!isEditable() || rowIndex !== activeRowIndex.value) {
      return ''
    }
    return 'is-active-row'
  }

  const focusCell = async (rowIndex, field) => {
    setActiveRow(rowIndex)
    await nextTick()
    const target = resolveFocusableElement(cellRefMap.get(buildCellKey(rowIndex, field)))
    target?.focus?.()
  }

  const focusFirstEditableCell = async () => {
    if (!isEditable()) {
      return
    }
    await focusCell(0, 'productSelect')
  }

  const focusNextCell = async (rowIndex, field) => {
    const currentFieldIndex = FOCUS_FIELDS.indexOf(field)
    if (currentFieldIndex < 0) {
      return
    }

    const nextField = FOCUS_FIELDS[currentFieldIndex + 1]
    if (nextField) {
      await focusCell(rowIndex, nextField)
      return
    }

    const nextRowIndex = rowIndex + 1
    if (nextRowIndex < getItems().length) {
      await focusCell(nextRowIndex, 'productSelect')
      return
    }

    onAppendRow?.()
    await focusCell(nextRowIndex, 'productSelect')
  }

  const focusPreviousCell = async (rowIndex, field) => {
    const currentFieldIndex = FOCUS_FIELDS.indexOf(field)
    if (currentFieldIndex < 0) {
      return
    }

    const previousField = FOCUS_FIELDS[currentFieldIndex - 1]
    if (previousField) {
      await focusCell(rowIndex, previousField)
      return
    }

    const previousRowIndex = rowIndex - 1
    if (previousRowIndex >= 0) {
      await focusCell(previousRowIndex, FOCUS_FIELDS[FOCUS_FIELDS.length - 1])
      return
    }

    await focusCell(rowIndex, field)
  }

  const handleCellKeydown = async (event, rowIndex, field) => {
    if (!isEditable() || event.key !== 'Tab') {
      return
    }
    event.preventDefault()
    if (event.shiftKey) {
      await focusPreviousCell(rowIndex, field)
      return
    }
    await focusNextCell(rowIndex, field)
  }

  watch(editable, (value) => {
    if (!value) {
      activeRowIndex.value = -1
    }
  })

  watch(
    () => getItems().length,
    (length) => {
      if (!isEditable() || length <= 0) {
        activeRowIndex.value = -1
        return
      }
      if (activeRowIndex.value >= length) {
        activeRowIndex.value = length - 1
      }
    }
  )

  return {
    setCellRef,
    setActiveRow,
    resolveRowClassName,
    focusFirstEditableCell,
    handleCellKeydown
  }
}
