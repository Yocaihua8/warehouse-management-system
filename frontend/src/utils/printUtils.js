export const openPrintWindow = (path) => {
    if (!path || typeof window === 'undefined') {
        return
    }
    window.open(path, '_blank')
}

const MAX_PRINT_ROWS = 8

const CN_NUM = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖']
const CN_INT_RADICE = ['', '拾', '佰', '仟']
const CN_INT_UNITS = ['', '万', '亿', '兆']
const CN_DEC_UNITS = ['角', '分']
const CN_INTEGER = '整'
const CN_INT_LAST = '元'
const MAX_NUMBER = 999999999999999.99

export const toChineseAmount = (value) => {
    const amount = Number(value)
    if (!Number.isFinite(amount)) {
        return '-'
    }
    if (amount === 0) {
        return `${CN_NUM[0]}${CN_INT_LAST}${CN_INTEGER}`
    }
    if (amount > MAX_NUMBER || amount < 0) {
        return '-'
    }

    const [integerPart, decimalPart = ''] = amount.toFixed(2).split('.')
    let chineseStr = ''

    if (Number(integerPart) > 0) {
        let zeroCount = 0
        const intLen = integerPart.length
        for (let i = 0; i < intLen; i++) {
            const n = integerPart.charAt(i)
            const p = intLen - i - 1
            const q = Math.floor(p / 4)
            const m = p % 4
            if (n === '0') {
                zeroCount++
            } else {
                if (zeroCount > 0) {
                    chineseStr += CN_NUM[0]
                }
                zeroCount = 0
                chineseStr += CN_NUM[Number(n)] + CN_INT_RADICE[m]
            }
            if (m === 0 && zeroCount < 4) {
                chineseStr += CN_INT_UNITS[q]
            }
        }
        chineseStr += CN_INT_LAST
    }

    const jiao = decimalPart.charAt(0)
    const fen = decimalPart.charAt(1)
    if (jiao !== '0') {
        chineseStr += CN_NUM[Number(jiao)] + CN_DEC_UNITS[0]
    }
    if (fen !== '0') {
        chineseStr += CN_NUM[Number(fen)] + CN_DEC_UNITS[1]
    }

    if (!chineseStr) {
        return `${CN_NUM[0]}${CN_INT_LAST}${CN_INTEGER}`
    }
    if (jiao === '0' && fen === '0') {
        chineseStr += CN_INTEGER
    }
    return chineseStr
}

export const padPrintItems = (rawItems) => {
    const normalized = Array.isArray(rawItems) ? rawItems : []
    const mapped = normalized.map((item, index) => ({
        index: index + 1,
        productName: item?.productName || '-',
        specification: item?.specification || '-',
        unit: item?.unit || '-',
        quantity: item?.quantity ?? '-',
        unitPrice: item?.unitPrice ?? '-',
        amount: item?.amount ?? '-',
        remark: item?.remark || '-',
        isEmpty: false
    }))

    while (mapped.length < MAX_PRINT_ROWS) {
        mapped.push({
            index: mapped.length + 1,
            productName: '',
            specification: '',
            unit: '',
            quantity: '',
            unitPrice: '',
            amount: '',
            remark: '',
            isEmpty: true
        })
    }

    return mapped
}
