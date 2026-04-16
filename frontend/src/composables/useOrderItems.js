export function useOrderItems(options) {
    const {
        form,
        createEmptyItem,
        minItems = 1,
        onMinItemsReached,
        onProductChange
    } = options || {}

    const addItem = () => {
        if (!form || typeof createEmptyItem !== 'function') {
            return
        }
        form.itemList.push(createEmptyItem())
    }

    const removeItem = (index) => {
        if (!form || !Array.isArray(form.itemList)) {
            return false
        }
        if (form.itemList.length <= minItems) {
            if (typeof onMinItemsReached === 'function') {
                onMinItemsReached()
            }
            return false
        }
        form.itemList.splice(index, 1)
        return true
    }

    const insertItem = (afterIndex) => {
        if (!form || typeof createEmptyItem !== 'function' || !Array.isArray(form.itemList)) {
            return
        }
        const index = Number(afterIndex)
        const safeIndex = Number.isFinite(index) ? Math.max(0, Math.min(index + 1, form.itemList.length)) : form.itemList.length
        form.itemList.splice(safeIndex, 0, createEmptyItem())
    }

    const updateRowField = (index, field, value) => {
        if (!form || !Array.isArray(form.itemList)) {
            return
        }
        const row = form.itemList[index]
        if (!row || !field) {
            return
        }
        row[field] = value
    }

    const selectProduct = (index, productId, productOptions = []) => {
        if (!form || !Array.isArray(form.itemList)) {
            return
        }
        const row = form.itemList[index]
        if (!row) {
            return
        }

        if (typeof onProductChange === 'function') {
            onProductChange(row, productId)
            return
        }

        const selectedProduct = productOptions.find(item => Number(item.id) === Number(productId))
        if (selectedProduct) {
            row.productId = productId
            row.productCode = selectedProduct.productCode || ''
            row.productName = selectedProduct.productName || ''
            row.specification = selectedProduct.specification || ''
            row.unit = selectedProduct.unit || ''
        } else {
            row.productId = productId
            row.productCode = ''
            row.productName = ''
            row.specification = ''
            row.unit = ''
        }
    }

    const resetItems = () => {
        if (!form || typeof createEmptyItem !== 'function') {
            return
        }
        form.itemList = [createEmptyItem()]
    }

    return {
        addItem,
        removeItem,
        insertItem,
        updateRowField,
        selectProduct,
        resetItems
    }
}
