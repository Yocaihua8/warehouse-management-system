export function useOrderCalc() {
    const calcAmount = (row) => {
        const quantity = Number(row?.quantity || 0)
        const unitPrice = Number(row?.unitPrice || 0)
        return (quantity * unitPrice).toFixed(2)
    }

    return {
        calcAmount
    }
}

