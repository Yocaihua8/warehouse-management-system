import { ORDER_TYPE } from '../utils/orderHelper'

export function useOrderValidation(emitMessage) {
    const notify = (message) => {
        if (typeof emitMessage === 'function') {
            emitMessage(message)
        }
    }

    const validateInboundForm = (form) => {
        if (!form?.supplierName || !String(form.supplierName).trim()) {
            notify('供应商名称不能为空')
            return false
        }

        if (!Array.isArray(form.itemList) || form.itemList.length === 0) {
            notify('入库明细不能为空')
            return false
        }

        for (let i = 0; i < form.itemList.length; i++) {
            const item = form.itemList[i]
            if (!item?.productId) {
                notify(`第 ${i + 1} 行商品不能为空`)
                return false
            }
            if (!item.quantity || Number(item.quantity) <= 0) {
                notify(`第 ${i + 1} 行数量必须大于 0`)
                return false
            }
            if (item.unitPrice === null || item.unitPrice === undefined || Number(item.unitPrice) < 0) {
                notify(`第 ${i + 1} 行单价不能小于 0`)
                return false
            }
        }

        return true
    }

    const validateOutboundForm = (form) => {
        if (!form?.customerId) {
            notify('请选择客户')
            return false
        }

        if (!Array.isArray(form.itemList) || form.itemList.length === 0) {
            notify('请至少添加一条出库明细')
            return false
        }

        for (const item of form.itemList) {
            if (!item?.productId) {
                notify('请选择商品')
                return false
            }
            if (!item.quantity || Number(item.quantity) <= 0) {
                notify('出库数量必须大于0')
                return false
            }
            if (item.unitPrice === null || item.unitPrice === undefined || Number(item.unitPrice) < 0) {
                notify('出库单价不能小于0')
                return false
            }
        }

        return true
    }

    const validateOrderForm = (orderType, form) => {
        if (orderType === ORDER_TYPE.OUTBOUND) {
            return validateOutboundForm(form)
        }
        return validateInboundForm(form)
    }

    const validateAiImportedItems = (items) => {
        if (!Array.isArray(items)) {
            notify('识别结果为空')
            return false
        }

        for (let i = 0; i < items.length; i++) {
            const item = items[i]
            if (!item?.productId && !item?.matchedProductId) {
                notify(`第 ${i + 1} 行商品尚未匹配，请先处理后再提交`)
                return false
            }
        }
        return true
    }

    return {
        validateInboundForm,
        validateOutboundForm,
        validateOrderForm,
        validateAiImportedItems
    }
}
