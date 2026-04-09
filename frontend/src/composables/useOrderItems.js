export function useOrderItems(options) {
    const {
        form,
        createEmptyItem,
        minItems = 1,
        onMinItemsReached
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

    const resetItems = () => {
        if (!form || typeof createEmptyItem !== 'function') {
            return
        }
        form.itemList = [createEmptyItem()]
    }

    return {
        addItem,
        removeItem,
        resetItems
    }
}

