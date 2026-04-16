export const ORDER_TYPE = {
    INBOUND: 'inbound',
    OUTBOUND: 'outbound'
}

export const PAGE_MODE = {
    CREATE: 'create',
    EDIT: 'edit',
    VIEW: 'view',
    CONFIRM: 'confirm',
    READONLY: 'readonly'
}

export const ORDER_STATUS = {
    DRAFT: 1,
    COMPLETED: 2,
    VOID: 3
}

export const parsePageData = (payload) => {
    if (Array.isArray(payload)) {
        return {
            list: payload,
            total: payload.length
        }
    }

    return {
        list: Array.isArray(payload?.list) ? payload.list : [],
        total: typeof payload?.total === 'number' ? payload.total : 0
    }
}

export const parseDraftId = (draftIdValue) => {
    const normalized = Number(draftIdValue)
    if (!Number.isFinite(normalized) || normalized <= 0) {
        return null
    }
    return normalized
}

export const displayText = (value) => {
    if (value === null || value === undefined) {
        return '-'
    }
    const normalized = String(value).trim()
    return normalized || '-'
}

export const today = () => new Date().toISOString().slice(0, 10)

export const createEmptyOrderItem = () => ({
    productId: null,
    productCode: '',
    productName: '',
    specification: '',
    unit: '',
    availableStock: null,
    quantity: 1,
    unitPrice: 0,
    remark: ''
})

export const createOrderItemList = (count = 1) => {
    const size = Number.isFinite(Number(count)) ? Math.max(1, Number(count)) : 1
    return Array.from({ length: size }, () => createEmptyOrderItem())
}
