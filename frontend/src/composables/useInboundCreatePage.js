import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { confirmInboundOrder, getInboundOrderDetail, saveInboundOrder, updateInboundOrderDraft } from '../api/inbound'
import { getProductList } from '../api/product'
import { useOrderCalc } from './useOrderCalc'
import { useOrderForm } from './useOrderForm'
import { useOrderItems } from './useOrderItems'
import { useOrderValidation } from './useOrderValidation'
import { useProductSearch } from './useProductSearch'
import { createEmptyOrderItem, createOrderItemList, ORDER_STATUS, ORDER_TYPE, PAGE_MODE, today } from '../utils/orderHelper'
import { getRole } from '../utils/auth'

export function useInboundCreatePage() {
  const route = useRoute()
  const router = useRouter()

  const aiDialogRef = ref(null)
  const initialSnapshot = ref('')

  const form = reactive({
    orderNo: '',
    orderDate: today(),
    sourceType: 'MANUAL',
    supplierName: '',
    remark: '',
    itemList: createOrderItemList(8)
  })

  const { calcAmount } = useOrderCalc()
  const { addItem, removeItem, insertItem, updateRowField } = useOrderItems({
    form,
    createEmptyItem: createEmptyOrderItem,
    minItems: 1,
    onMinItemsReached: () => ElMessage.warning('至少保留一条入库明细')
  })
  const { validateOrderForm } = useOrderValidation((message) => ElMessage.error(message))
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
    initialItemCount: 8,
    defaultValues: { orderNo: '', orderDate: today(), sourceType: 'MANUAL', supplierName: '', remark: '' },
    orderType: ORDER_TYPE.INBOUND
  })

  const {
    productOptions,
    productLoading,
    loadProducts: loadProductOptions,
    handleProductSearch,
    handleProductChange
  } = useProductSearch({
    fetchProductList: getProductList,
    onError: (message) => ElMessage.error(message)
  })

  const onProductChange = ({ row, productId }) => {
    handleProductChange(row, productId)
  }

  const buildPayload = () => ({
    supplierName: form.supplierName.trim(),
    remark: form.remark,
    itemList: form.itemList.map(item => ({
      productId: Number(item.productId),
      quantity: Number(item.quantity),
      unitPrice: Number(item.unitPrice),
      remark: item.remark
    }))
  })

  const buildFormSnapshot = () => {
    return JSON.stringify({
      orderDate: form.orderDate || '',
      supplierName: (form.supplierName || '').trim(),
      remark: form.remark || '',
      itemList: (form.itemList || []).map(item => ({
        productId: item.productId != null ? Number(item.productId) : null,
        quantity: Number(item.quantity || 0),
        unitPrice: Number(item.unitPrice || 0),
        remark: item.remark || ''
      }))
    })
  }

  const refreshSnapshot = () => {
    initialSnapshot.value = buildFormSnapshot()
  }

  const hasUnsavedChanges = computed(() => buildFormSnapshot() !== initialSnapshot.value)

  const loadInboundDraft = async (draftIdValue) => {
    const draftId = parseDraftId(draftIdValue)
    if (!draftId) {
      markCreateMode()
      return
    }

    try {
      const res = await getInboundOrderDetail(draftId)
      if (res.data?.code !== 1) {
        ElMessage.error(res.data?.message || '加载入库草稿失败')
        return
      }

      const detail = res.data?.data
      if (!detail) {
        ElMessage.error('入库草稿不存在')
        return
      }
      if (Number(detail.orderStatus) !== ORDER_STATUS.DRAFT) {
        ElMessage.warning('仅草稿状态允许编辑，已为你跳转到详情页')
        await router.replace(`/inbound/detail/${draftId}`)
        return
      }

      currentDraftId.value = draftId
      resolvePageModeByStatus(detail.orderStatus)
      applyDraftDetail(detail, (source, target) => {
        target.orderNo = source.orderNo || ''
        target.orderDate = (source.createdTime || '').slice(0, 10) || today()
        target.sourceType = source.sourceType || 'MANUAL'
        target.supplierName = source.supplierName || ''
        target.remark = source.remark || ''
      })
      refreshSnapshot()
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || error?.message || '加载入库草稿失败')
    }
  }

  const saveDraft = async ({ andCreateNew = false } = {}) => {
    if (!validateOrderForm(ORDER_TYPE.INBOUND, form)) {
      return false
    }

    try {
      submitting.value = true
      const payload = buildPayload()
      const isEditMode = !!currentDraftId.value
      const res = isEditMode
        ? await updateInboundOrderDraft(currentDraftId.value, payload)
        : await saveInboundOrder(payload)

      if (res.data?.code !== 1) {
        ElMessage.error(res.data?.message || '保存失败')
        return false
      }

      if (isEditMode) {
        ElMessage.success(res.data?.data || '更新草稿成功')
      } else {
        const createdOrder = res.data?.data
        const orderNo = createdOrder?.orderNo
        const orderId = createdOrder?.id
        ElMessage.success(orderNo
          ? `保存草稿成功，单号：${orderNo}`
          : (orderId ? `保存草稿成功，ID=${orderId}` : '保存草稿成功'))
      }

      resetForm()
      markCreateMode()
      refreshSnapshot()
      if (!andCreateNew) {
        await router.push('/inbound/list')
      }
      return true
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || error?.message || '保存失败')
      return false
    } finally {
      submitting.value = false
    }
  }

  const handleSubmit = async () => {
    await saveDraft({ andCreateNew: false })
  }

  const handleSaveAndNew = async () => {
    await saveDraft({ andCreateNew: true })
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
    if (!currentDraftId.value) {
      return
    }
    window.open(`/inbound/print/${currentDraftId.value}`, '_blank')
  }

  const handleSubmitConfirm = async () => {
    if (!currentDraftId.value) {
      ElMessage.warning('请先保存草稿再确认入库')
      return
    }
    if (getRole() !== 'ADMIN') {
      ElMessage.warning('仅管理员可执行确认入库')
      return
    }
    try {
      await ElMessageBox.confirm('确认后将正式入库并增加库存，是否继续？', '确认入库', {
        type: 'warning',
        confirmButtonText: '确认',
        cancelButtonText: '取消'
      })
      const res = await confirmInboundOrder(currentDraftId.value)
      if (res.data?.code === 1) {
        ElMessage.success(res.data?.data || '确认入库成功')
        await router.push(`/inbound/detail/${currentDraftId.value}`)
      } else {
        ElMessage.error(res.data?.message || '确认入库失败')
      }
    } catch (error) {
      if (error === 'cancel' || error === 'close') {
        return
      }
      ElMessage.error(error?.response?.data?.message || error?.message || '确认入库失败')
    }
  }

  const editable = computed(() => pageMode.value !== PAGE_MODE.READONLY)
  const canSaveDraft = computed(() => editable.value)
  const canSaveAndNew = computed(() => editable.value)
  const canClear = computed(() => editable.value)
  const canAiImport = computed(() => editable.value)
  const canSubmitConfirm = computed(() => {
    return getRole() === 'ADMIN' &&
      pageMode.value === PAGE_MODE.EDIT &&
      Boolean(currentDraftId.value)
  })
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
      await loadInboundDraft(draftId)
    },
    { immediate: true }
  )

  onMounted(() => {
    loadProductOptions()
    refreshSnapshot()
    window.addEventListener('beforeunload', handleBeforeUnload)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('beforeunload', handleBeforeUnload)
  })

  return {
    aiDialogRef,
    form,
    pageMode,
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
  }
}
