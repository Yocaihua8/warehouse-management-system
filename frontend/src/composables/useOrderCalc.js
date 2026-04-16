import { computed, isRef, unref } from 'vue'

export function useOrderCalc() {
    const calcAmount = (row) => {
        const quantity = Number(row?.quantity || 0)
        const unitPrice = Number(row?.unitPrice || 0)
        return (quantity * unitPrice).toFixed(2)
    }

    const calcTotals = (items) => {
        const rows = Array.isArray(items) ? items : []
        const totalQuantity = rows.reduce((sum, row) => sum + Number(row?.quantity || 0), 0)
        const totalAmount = rows.reduce((sum, row) => {
            return sum + Number(row?.quantity || 0) * Number(row?.unitPrice || 0)
        }, 0)

        return {
            totalQuantity,
            totalAmount: totalAmount.toFixed(2)
        }
    }

    const useComputedTotals = (items) => {
        if (!isRef(items)) {
            return {
                totalQuantity: computed(() => calcTotals(items).totalQuantity),
                totalAmount: computed(() => calcTotals(items).totalAmount)
            }
        }
        return {
            totalQuantity: computed(() => calcTotals(unref(items)).totalQuantity),
            totalAmount: computed(() => calcTotals(unref(items)).totalAmount)
        }
    }

    return {
        calcAmount,
        calcTotals,
        useComputedTotals
    }
}
