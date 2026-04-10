import { computed, ref } from 'vue'
import { ORDER_STATUS, PAGE_MODE, parseDraftId } from '../utils/orderHelper'

export function useOrderForm(options) {
    const {
        form,
        createEmptyItem,
        defaultValues = {},
        initialItemCount = 1,
        orderType = 'inbound'
    } = options || {}

    const pageMode = ref(PAGE_MODE.CREATE)
    const currentDraftId = ref(null)
    const submitting = ref(false)
    const pageTitle = computed(() => {
        if (pageMode.value === PAGE_MODE.READONLY) {
            return orderType === 'outbound' ? '出库单详情' : '入库单详情'
        }
        if (currentDraftId.value) {
            return orderType === 'outbound' ? '编辑出库草稿' : '编辑入库草稿'
        }
        return orderType === 'outbound' ? '新增出库单' : '新增入库单'
    })

    const buildInitialItems = () => {
        const count = Number.isFinite(Number(initialItemCount)) ? Math.max(1, Number(initialItemCount)) : 1
        return Array.from({ length: count }, () => createEmptyItem())
    }

    const resetForm = () => {
        if (!form || typeof createEmptyItem !== 'function') {
            return
        }
        Object.keys(defaultValues).forEach((key) => {
            form[key] = defaultValues[key]
        })
        form.itemList = buildInitialItems()
    }

    const applyDraftDetail = (detail, mapHeader) => {
        if (!detail || !form || typeof createEmptyItem !== 'function') {
            return
        }
        if (typeof mapHeader === 'function') {
            mapHeader(detail, form)
        }
        form.itemList = Array.isArray(detail.itemList) && detail.itemList.length > 0
            ? detail.itemList.map(item => ({
                productId: item.productId != null ? Number(item.productId) : null,
                productCode: item.productCode || '',
                productName: item.productName || '',
                specification: item.specification || '',
                unit: item.unit || '',
                availableStock: item.availableStock != null ? Number(item.availableStock) : null,
                quantity: Number(item.quantity || 1),
                unitPrice: Number(item.unitPrice || 0),
                remark: item.remark || ''
            }))
            : buildInitialItems()
    }

    const resolvePageModeByStatus = (orderStatus) => {
        const status = Number(orderStatus)
        if (status === ORDER_STATUS.DRAFT) {
            pageMode.value = PAGE_MODE.EDIT
            return pageMode.value
        }
        pageMode.value = PAGE_MODE.READONLY
        return pageMode.value
    }

    const markCreateMode = () => {
        pageMode.value = PAGE_MODE.CREATE
        currentDraftId.value = null
    }

    return {
        resetForm,
        parseDraftId,
        pageMode,
        currentDraftId,
        submitting,
        pageTitle,
        applyDraftDetail,
        resolvePageModeByStatus,
        markCreateMode
    }
}
