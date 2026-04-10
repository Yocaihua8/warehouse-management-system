import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { getCustomerList } from '../api/customer'
import { getProductList } from '../api/product'
import { getStockList } from '../api/stock'
import { addOutboundOrder, confirmOutboundOrder, getOutboundDetail, updateOutboundOrderDraft } from '../api/outbound'
import { useOrderForm } from './useOrderForm'
import { useOrderItems } from './useOrderItems'
import { useOrderCalc } from './useOrderCalc'
import { useOrderValidation } from './useOrderValidation'
import { useProductSearch } from './useProductSearch'
import { createEmptyOrderItem, createOrderItemList, ORDER_STATUS, ORDER_TYPE, PAGE_MODE, parsePageData, today } from '../utils/orderHelper'
import { getRole } from '../utils/auth'

export function useOutboundCreatePage() {
  const route = useRoute()
  const router = useRouter()

  const aiDialogRef = ref(null)
  const customerOptions = ref([])
  const initialSnapshot = ref('')

  const form = reactive({
    orderNo: '',
    orderDate: today(),
    sourceType: 'MANUAL',
    customerId: null,
    remark: '',
    itemList: createOrderItemList(8)
  })

  const { addItem, removeItem, insertItem, updateRowField } = useOrderItems({
    form,
    createEmptyItem: createEmptyOrderItem,
    minItems: 1,
    onMinItemsReached: () => ElMessage.warning('至少保留一条出库明细')
  })
  const { calcAmount } = useOrderCalc()
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
    defaultValues: {
      orderNo: '',
      orderDate: today(),
      sourceType: 'MANUAL',
      customerId: null,
      remark: ''
    },
    orderType: ORDER_TYPE.OUTBOUND
  })
  const { validateOrderForm } = useOrderValidation((message) => ElMessage.warning(message))
  const {
    productOptions,
    productLoading,
    loadProducts,
    handleProductSearch,
    handleProductChange
  } = useProductSearch({
    fetchProductList: getProductList,
    onError: (message) => ElMessage.error(message)
  })

  const loadCustomers = async () => {
    try {
      const res = await getCustomerList({ pageNum: 1, pageSize: 200 })
      customerOptions.value = res.data?.code === 1 ? parsePageData(res.data.data).list : []
      if (res.data?.code !== 1) {
        ElMessage.error(res.data?.message || '加载客户列表失败')
      }
    } catch (error) {
      customerOptions.value = []
      ElMessage.error(error?.response?.data?.message || error?.message || '请求客户列表接口失败')
    }
  }

  const onProductChange = ({ row, productId }) => {
    handleProductChange(row, productId)
    loadAvailableStock(row, productId)
  }

  const loadAvailableStock = async (row, productId) => {
    if (!row || !productId) {
      row.availableStock = null
      return
    }

    try {
      const selectedProduct = productOptions.value.find(item => Number(item.id) === Number(productId))
      if (!selectedProduct?.productCode) {
        row.availableStock = null
        return
      }

      const res = await getStockList({
        productCode: selectedProduct.productCode,
        pageNum: 1,
        pageSize: 20
      })
      if (res.data?.code !== 1) {
        row.availableStock = null
        return
      }

      const stockList = parsePageData(res.data?.data).list
      const matched = stockList.find(item => Number(item.productId) === Number(productId))
      row.availableStock = matched?.quantity != null ? Number(matched.quantity) : 0
    } catch (error) {
      row.availableStock = null
    }
  }

  const refreshItemStocks = async () => {
    const tasks = (form.itemList || [])
      .filter(item => item?.productId)
      .map(item => loadAvailableStock(item, item.productId))
    if (tasks.length > 0) {
      await Promise.all(tasks)
    }
  }

  const buildFormSnapshot = () => {
    return JSON.stringify({
      orderDate: form.orderDate || '',
      customerId: form.customerId != null ? Number(form.customerId) : null,
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

  const loadOutboundDraft = async (draftIdValue) => {
    const draftId = parseDraftId(draftIdValue)
    if (!draftId) {
      markCreateMode()
      return
    }

    try {
      const res = await getOutboundDetail(draftId)
      if (res.data?.code !== 1) {
        ElMessage.error(res.data?.message || '加载出库草稿失败')
        return
      }

      const detail = res.data?.data
      if (!detail) {
        ElMessage.error('出库草稿不存在')
        return
      }
      if (Number(detail.orderStatus) !== ORDER_STATUS.DRAFT) {
        ElMessage.warning('仅草稿状态允许编辑，已为你跳转到详情页')
        await router.replace(`/outbound/detail/${draftId}`)
        return
      }

      currentDraftId.value = draftId
      resolvePageModeByStatus(detail.orderStatus)
      applyDraftDetail(detail, (source, target) => {
        target.orderNo = source.orderNo || ''
        target.orderDate = (source.createdTime || '').slice(0, 10) || today()
        target.sourceType = source.sourceType || 'MANUAL'
        target.customerId = source.customerId != null ? Number(source.customerId) : null
        target.remark = source.remark || ''
      })
      await refreshItemStocks()
      refreshSnapshot()
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || error?.message || '加载出库草稿失败')
    }
  }

  const saveDraft = async ({ andCreateNew = false } = {}) => {
    if (!validateOrderForm(ORDER_TYPE.OUTBOUND, form)) {
      return false
    }

    try {
      submitting.value = true
      const payload = {
        customerId: form.customerId,
        remark: form.remark,
        itemList: form.itemList.map(item => ({
          productId: item.productId,
          quantity: item.quantity,
          unitPrice: item.unitPrice,
          remark: item.remark
        }))
      }

      const isEditMode = !!currentDraftId.value
      const res = isEditMode
        ? await updateOutboundOrderDraft(currentDraftId.value, payload)
        : await addOutboundOrder(payload)

      if (res.data?.code !== 1) {
        ElMessage.error(res.data?.message || '保存出库草稿失败')
        return false
      }

      if (isEditMode) {
        ElMessage.success(res.data?.data || '更新出库草稿成功')
      } else {
        const createdOrder = res.data?.data
        const orderNo = createdOrder?.orderNo
        const orderId = createdOrder?.id
        ElMessage.success(orderNo
          ? `保存出库单草稿成功，单号：${orderNo}`
          : (orderId ? `保存出库单草稿成功，ID=${orderId}` : '保存出库单草稿成功'))
      }

      resetForm()
      markCreateMode()
      refreshSnapshot()
      if (!andCreateNew) {
        await router.push('/outbound/list')
      }
      return true
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || error?.message || '保存出库草稿失败')
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
    window.open(`/outbound/print/${currentDraftId.value}`, '_blank')
  }

  const handleSubmitConfirm = async () => {
    if (!currentDraftId.value) {
      ElMessage.warning('请先保存草稿再确认出库')
      return
    }
    if (getRole() !== 'ADMIN') {
      ElMessage.warning('仅管理员可执行确认出库')
      return
    }
    try {
      await ElMessageBox.confirm('确认后将正式出库并扣减库存，是否继续？', '确认出库', {
        type: 'warning',
        confirmButtonText: '确认',
        cancelButtonText: '取消'
      })
      const res = await confirmOutboundOrder(currentDraftId.value)
      if (res.data?.code === 1) {
        ElMessage.success(res.data?.data || '确认出库成功')
        await router.push(`/outbound/detail/${currentDraftId.value}`)
      } else {
        ElMessage.error(res.data?.message || '确认出库失败')
      }
    } catch (error) {
      if (error === 'cancel' || error === 'close') {
        return
      }
      ElMessage.error(error?.response?.data?.message || error?.message || '确认出库失败')
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
      await loadOutboundDraft(draftId)
    },
    { immediate: true }
  )

  onMounted(() => {
    loadCustomers()
    loadProducts()
    refreshItemStocks()
    refreshSnapshot()
    window.addEventListener('beforeunload', handleBeforeUnload)
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
    customerOptions,
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
