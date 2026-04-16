import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { useOrderCalc } from './useOrderCalc'
import { useOrderForm } from './useOrderForm'
import { useOrderItems } from './useOrderItems'
import { useOrderValidation } from './useOrderValidation'
import { useProductSearch } from './useProductSearch'
import { createEmptyOrderItem, createOrderItemList, ORDER_STATUS, PAGE_MODE } from '../utils/orderHelper'
import { getRole } from '../utils/auth'

const noop = () => {}

const createDefaultMessages = () => ({
  minItemsReached: '至少保留一条明细',
  loadDraftFailed: '加载草稿失败',
  draftNotFound: '草稿不存在',
  readonlyRedirect: '仅草稿状态允许编辑，已为你跳转到详情页',
  saveFailed: '保存失败',
  updateSuccess: '更新草稿成功',
  createSuccess: ({ createdOrder }) => {
    const orderNo = createdOrder?.orderNo
    const orderId = createdOrder?.id
    if (orderNo) {
      return `保存草稿成功，单号：${orderNo}`
    }
    if (orderId) {
      return `保存草稿成功，ID=${orderId}`
    }
    return '保存草稿成功'
  },
  missingDraftIdAfterSave: '草稿保存成功，但未返回单据ID，无法继续执行后续操作',
  confirmPermissionDenied: '仅管理员可执行确认操作',
  confirmTitle: '确认单据',
  confirmMessage: '确认后将正式提交单据，是否继续？',
  confirmSuccess: '确认成功',
  confirmFailed: '确认失败'
})

export function useOrderWorkbenchPage(options = {}) {
  const {
    orderType,
    defaultValues = {},
    initialItemCount = 8,
    fetchProductList,
    fetchDetail,
    saveDraftApi,
    updateDraftApi,
    confirmApi,
    buildPayload,
    buildSnapshot,
    mapDetailHeader,
    listRoute,
    detailRouteBuilder,
    printRouteBuilder,
    loadInitialData,
    afterDraftLoaded,
    afterProductsLoaded,
    onProductSelected,
    messages: rawMessages = {},
    notify: rawNotify = {}
  } = options

  const route = useRoute()
  const router = useRouter()

  const messages = { ...createDefaultMessages(), ...rawMessages }
  const notify = {
    success: typeof rawNotify.success === 'function' ? rawNotify.success : noop,
    error: typeof rawNotify.error === 'function' ? rawNotify.error : noop,
    warning: typeof rawNotify.warning === 'function' ? rawNotify.warning : noop,
    validation: typeof rawNotify.validation === 'function'
      ? rawNotify.validation
      : (typeof rawNotify.error === 'function' ? rawNotify.error : noop)
  }

  const aiDialogRef = ref(null)
  const initialSnapshot = ref('')

  const form = reactive({
    ...defaultValues,
    itemList: createOrderItemList(initialItemCount)
  })

  const { calcAmount, useComputedTotals } = useOrderCalc()
  const {
    resetForm,
    parseDraftId,
    currentDraftId,
    pageMode,
    submitting,
    pageTitle,
    applyDraftDetail,
    resolvePageModeByStatus,
    markCreateMode
  } = useOrderForm({
    form,
    createEmptyItem: createEmptyOrderItem,
    initialItemCount,
    defaultValues,
    orderType
  })

  const { addItem, removeItem, insertItem, updateRowField } = useOrderItems({
    form,
    createEmptyItem: createEmptyOrderItem,
    minItems: 1,
    onMinItemsReached: () => notify.warning(messages.minItemsReached)
  })

  const { validateOrderForm } = useOrderValidation((message) => notify.validation(message))
  const {
    productOptions,
    productLoading,
    loadProducts,
    handleProductSearch,
    handleProductChange
  } = useProductSearch({
    fetchProductList,
    onError: (message) => notify.error(message)
  })

  const { totalQuantity, totalAmount } = useComputedTotals(computed(() => form.itemList))
  const summary = computed(() => ({
    totalQuantity: totalQuantity.value,
    totalAmount: totalAmount.value
  }))

  const buildFormSnapshot = () => {
    if (typeof buildSnapshot === 'function') {
      return buildSnapshot(form)
    }
    return JSON.stringify(form)
  }

  const refreshSnapshot = () => {
    initialSnapshot.value = buildFormSnapshot()
  }

  const hasUnsavedChanges = computed(() => buildFormSnapshot() !== initialSnapshot.value)

  const loadDraft = async (draftIdValue) => {
    const draftId = parseDraftId(draftIdValue)
    if (!draftId) {
      markCreateMode()
      return
    }

    try {
      const res = await fetchDetail(draftId)
      if (res.data?.code !== 1) {
        notify.error(res.data?.message || messages.loadDraftFailed)
        return
      }

      const detail = res.data?.data
      if (!detail) {
        notify.error(messages.draftNotFound)
        return
      }

      if (Number(detail.orderStatus) !== ORDER_STATUS.DRAFT) {
        notify.warning(messages.readonlyRedirect)
        if (typeof detailRouteBuilder === 'function') {
          await router.replace(detailRouteBuilder(draftId))
        }
        return
      }

      currentDraftId.value = draftId
      resolvePageModeByStatus(detail.orderStatus)
      applyDraftDetail(detail, mapDetailHeader)
      if (typeof afterDraftLoaded === 'function') {
        await afterDraftLoaded({
          detail,
          form,
          productOptions: productOptions.value,
          currentDraftId: draftId
        })
      }
      refreshSnapshot()
    } catch (error) {
      notify.error(error?.response?.data?.message || error?.message || messages.loadDraftFailed)
    }
  }

  const saveDraft = async ({ afterSave = 'list', showSuccessMessage = true } = {}) => {
    if (!validateOrderForm(orderType, form)) {
      return null
    }

    try {
      submitting.value = true
      const payload = typeof buildPayload === 'function' ? buildPayload(form) : {}
      const isEditMode = !!currentDraftId.value
      const res = isEditMode
        ? await updateDraftApi(currentDraftId.value, payload)
        : await saveDraftApi(payload)

      if (res.data?.code !== 1) {
        notify.error(res.data?.message || messages.saveFailed)
        return null
      }

      const createdOrder = !isEditMode ? res.data?.data : null
      const savedDraftId = isEditMode
        ? currentDraftId.value
        : parseDraftId(createdOrder?.id)
      const savedOrderNo = isEditMode
        ? form.orderNo
        : (createdOrder?.orderNo || '')

      if (showSuccessMessage && isEditMode) {
        notify.success(typeof messages.updateSuccess === 'function' ? messages.updateSuccess({ res }) : messages.updateSuccess)
      } else if (showSuccessMessage) {
        notify.success(typeof messages.createSuccess === 'function'
          ? messages.createSuccess({ createdOrder, res })
          : messages.createSuccess)
      }

      if (afterSave === 'stay') {
        if (!savedDraftId) {
          notify.error(messages.missingDraftIdAfterSave)
          return null
        }
        currentDraftId.value = savedDraftId
        if (savedOrderNo) {
          form.orderNo = savedOrderNo
        }
        resolvePageModeByStatus(ORDER_STATUS.DRAFT)
        refreshSnapshot()
        return savedDraftId
      }

      resetForm()
      markCreateMode()
      refreshSnapshot()
      if (afterSave !== 'new' && listRoute) {
        await router.push(listRoute)
      }
      return savedDraftId
    } catch (error) {
      notify.error(error?.response?.data?.message || error?.message || messages.saveFailed)
      return null
    } finally {
      submitting.value = false
    }
  }

  const handleSubmit = async () => {
    return await saveDraft({ afterSave: 'list' })
  }

  const handleSaveAndNew = async () => {
    return await saveDraft({ afterSave: 'new' })
  }

  const onProductChange = async ({ row, productId, index }) => {
    handleProductChange(row, productId)
    if (typeof onProductSelected === 'function') {
      await onProductSelected({
        row,
        productId,
        index,
        form,
        productOptions: productOptions.value
      })
    }
  }

  const openAiDialog = () => {
    aiDialogRef.value?.openDialog?.()
  }

  const handleClear = () => {
    resetForm()
    markCreateMode()
    refreshSnapshot()
  }

  const openPrintPreview = () => {
    if (!currentDraftId.value || typeof printRouteBuilder !== 'function') {
      return
    }
    window.open(printRouteBuilder(currentDraftId.value), '_blank')
  }

  const handleSubmitConfirm = async () => {
    if (getRole() !== 'ADMIN') {
      notify.warning(messages.confirmPermissionDenied)
      return
    }

    try {
      await ElMessageBox.confirm(messages.confirmMessage, messages.confirmTitle, {
        type: 'warning',
        confirmButtonText: '确认',
        cancelButtonText: '取消'
      })
      const draftId = await saveDraft({ afterSave: 'stay', showSuccessMessage: false })
      if (!draftId) {
        return
      }
      const res = await confirmApi(draftId)
      if (res.data?.code === 1) {
        notify.success(res.data?.data || messages.confirmSuccess)
        if (typeof detailRouteBuilder === 'function') {
          await router.push(detailRouteBuilder(draftId))
        }
      } else {
        notify.error(res.data?.message || messages.confirmFailed)
      }
    } catch (error) {
      if (error === 'cancel' || error === 'close') {
        return
      }
      notify.error(error?.response?.data?.message || error?.message || messages.confirmFailed)
    }
  }

  const editable = computed(() => pageMode.value !== PAGE_MODE.READONLY)
  const canSaveDraft = computed(() => editable.value)
  const canSaveAndNew = computed(() => editable.value)
  const canClear = computed(() => editable.value)
  const canAiImport = computed(() => editable.value)
  const canSubmitConfirm = computed(() => getRole() === 'ADMIN' && editable.value)
  const canPrintPreview = computed(() => Boolean(currentDraftId.value))

  const handleBeforeUnload = (event) => {
    if (!hasUnsavedChanges.value) {
      return
    }
    event.preventDefault()
    event.returnValue = ''
  }

  onBeforeRouteLeave(async () => {
    if (!hasUnsavedChanges.value || submitting.value) {
      return true
    }
    try {
      await ElMessageBox.confirm('当前草稿有未保存修改，确认离开吗？', '离开提示', {
        type: 'warning',
        confirmButtonText: '离开',
        cancelButtonText: '继续编辑'
      })
      return true
    } catch (error) {
      return false
    }
  })

  watch(
    () => route.query.draftId,
    async (draftId) => {
      if (!draftId) {
        markCreateMode()
        refreshSnapshot()
        return
      }
      await loadDraft(draftId)
    },
    { immediate: true }
  )

  const initializePage = async () => {
    const tasks = [loadProducts()]
    if (typeof loadInitialData === 'function') {
      tasks.push(loadInitialData({ form, route, router }))
    }
    await Promise.all(tasks)
    if (typeof afterProductsLoaded === 'function') {
      await afterProductsLoaded({ form, productOptions: productOptions.value })
    }
  }

  onMounted(() => {
    refreshSnapshot()
    window.addEventListener('beforeunload', handleBeforeUnload)
    void initializePage()
  })

  onBeforeUnmount(() => {
    window.removeEventListener('beforeunload', handleBeforeUnload)
  })

  return {
    aiDialogRef,
    form,
    editable,
    pageTitle,
    submitting,
    currentDraftId,
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
    handleSaveAndNew,
    handleSubmitConfirm,
    openPrintPreview,
    canSaveDraft,
    canSaveAndNew,
    canClear,
    canAiImport,
    canSubmitConfirm,
    canPrintPreview,
    saveDraft,
    refreshSnapshot
  }
}
