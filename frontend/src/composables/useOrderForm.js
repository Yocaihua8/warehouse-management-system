import { parseDraftId } from '../utils/orderHelper'

export function useOrderForm(options) {
    const {
        form,
        createEmptyItem,
        defaultValues = {}
    } = options || {}

    const resetForm = () => {
        if (!form || typeof createEmptyItem !== 'function') {
            return
        }
        Object.keys(defaultValues).forEach((key) => {
            form[key] = defaultValues[key]
        })
        form.itemList = [createEmptyItem()]
    }

    return {
        resetForm,
        parseDraftId
    }
}

