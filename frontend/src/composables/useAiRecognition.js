import { ref } from 'vue'

export function useAiRecognition(options = {}) {
  const confirmRemark = options.confirmRemark || 'AI识别确认生成入库单'
  const normalizeItem = typeof options.normalizeItem === 'function'
    ? options.normalizeItem
    : ((item = {}, index = 0) => ({
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
    }))

  const aiDialogVisible = ref(false)
  const aiRecognizing = ref(false)
  const aiConfirming = ref(false)
  const aiUploadFile = ref(null)
  const aiDraft = ref(null)
  const aiManualReviewed = ref(false)

  const normalizeAiDraft = (draft) => {
    if (!draft) {
      return null
    }

    if (typeof options.normalizeDraft === 'function') {
      return options.normalizeDraft(draft, { normalizeItem, confirmRemark })
    }

    const normalizedItemList = Array.isArray(draft.itemList)
      ? draft.itemList.map((item, index) => normalizeItem(item, index))
      : []

    return {
      ...draft,
      supplierName: draft.supplierName || '',
      matchedSupplierId: draft.matchedSupplierId ?? null,
      supplierMatchStatus: draft.supplierMatchStatus || 'unmatched',
      rawText: draft.rawText || '',
      remark: draft.remark || confirmRemark,
      itemList: normalizedItemList.length > 0 ? normalizedItemList : [normalizeItem({}, 0)]
    }
  }

  const resetAiDraftState = () => {
    aiDraft.value = null
    aiUploadFile.value = null
    aiManualReviewed.value = false
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

  const addAiItem = () => {
    if (!aiDraft.value) {
      return
    }
    aiDraft.value.itemList.push(normalizeItem({}, aiDraft.value.itemList.length))
    markAiDraftDirty()
  }

  const removeAiItem = (index) => {
    if (!aiDraft.value || !Array.isArray(aiDraft.value.itemList)) {
      return false
    }
    if (aiDraft.value.itemList.length === 1) {
      return false
    }
    aiDraft.value.itemList.splice(index, 1)
    aiDraft.value.itemList.forEach((item, itemIndex) => {
      item.lineNo = itemIndex + 1
    })
    markAiDraftDirty()
    return true
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

  const buildAiConfirmPayload = () => {
    if (typeof options.buildConfirmPayload === 'function') {
      return options.buildConfirmPayload(aiDraft.value, { confirmRemark })
    }
    return {
      recordId: aiDraft.value?.recordId,
      supplierId: aiDraft.value?.matchedSupplierId != null ? Number(aiDraft.value.matchedSupplierId) : null,
      supplierName: aiDraft.value?.supplierName || '',
      rawText: aiDraft.value?.rawText || '',
      remark: aiDraft.value?.remark || confirmRemark,
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
    }
  }

  return {
    aiDialogVisible,
    aiRecognizing,
    aiConfirming,
    aiUploadFile,
    aiDraft,
    aiManualReviewed,
    normalizeAiDraft,
    normalizeInboundAiDraft: normalizeAiDraft,
    markAiDraftDirty,
    getAiWarningText,
    addAiItem,
    removeAiItem,
    hasUnmatchedAiItems,
    hasInvalidAiItems,
    buildAiConfirmPayload,
    resetAiDraftState
  }
}
