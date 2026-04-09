const DIGIT_MAP = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖']
const SMALL_UNIT_MAP = ['', '拾', '佰', '仟']
const SECTION_UNIT_MAP = ['', '万', '亿', '兆']

export const formatPrintValue = (value) => {
  if (value === null || value === undefined || value === '') {
    return ''
  }
  return value
}

export const displayCell = (value) => {
  if (value === null || value === undefined || value === '') {
    return '\u00A0'
  }
  return value
}

export const buildPrintItems = (list, minRows = 12) => {
  const source = Array.isArray(list) ? list : []
  const rows = source.map((item, index) => ({
    index: index + 1,
    productName: formatPrintValue(item.productName),
    specification: formatPrintValue(item.specification),
    unit: formatPrintValue(item.unit),
    quantity: formatPrintValue(item.quantity),
    unitPrice: formatPrintValue(item.unitPrice),
    amount: formatPrintValue(item.amount),
    remark: formatPrintValue(item.remark),
    isEmpty: false
  }))

  while (rows.length < minRows) {
    rows.push({
      index: '',
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
  return rows
}

const convertSection = (sectionNum) => {
  let sectionText = ''
  let zeroFlag = true
  let unitPos = 0
  let num = sectionNum

  while (num > 0) {
    const digit = num % 10
    if (digit === 0) {
      if (!zeroFlag) {
        zeroFlag = true
        sectionText = DIGIT_MAP[0] + sectionText
      }
    } else {
      zeroFlag = false
      sectionText = DIGIT_MAP[digit] + SMALL_UNIT_MAP[unitPos] + sectionText
    }
    unitPos += 1
    num = Math.floor(num / 10)
  }

  return sectionText.replace(/零+/g, '零').replace(/零$/g, '')
}

export const toChineseAmount = (value) => {
  if (value === null || value === undefined || value === '') {
    return '-'
  }

  const amount = Number(value)
  if (!Number.isFinite(amount)) {
    return '-'
  }
  if (amount === 0) {
    return '零元整'
  }

  const safeAmount = Math.round(amount * 100) / 100
  const integerPart = Math.floor(safeAmount)
  const fractionPart = Math.round((safeAmount - integerPart) * 100)

  let integerText = ''
  let sectionIndex = 0
  let currentInteger = integerPart
  let needZero = false

  while (currentInteger > 0) {
    const sectionNum = currentInteger % 10000
    if (sectionNum === 0) {
      if (integerText && !integerText.startsWith('零')) {
        needZero = true
      }
    } else {
      let sectionText = convertSection(sectionNum)
      if (needZero) {
        sectionText = '零' + sectionText
        needZero = false
      }
      integerText = sectionText + SECTION_UNIT_MAP[sectionIndex] + integerText
      if (sectionNum < 1000) {
        needZero = true
      }
    }
    sectionIndex += 1
    currentInteger = Math.floor(currentInteger / 10000)
  }

  integerText = (integerText || '零').replace(/零+/g, '零').replace(/零$/g, '') + '元'

  if (fractionPart === 0) {
    return integerText + '整'
  }

  const jiao = Math.floor(fractionPart / 10)
  const fen = fractionPart % 10
  let fractionText = ''

  if (jiao > 0) {
    fractionText += DIGIT_MAP[jiao] + '角'
  }
  if (fen > 0) {
    if (jiao === 0) {
      fractionText += '零'
    }
    fractionText += DIGIT_MAP[fen] + '分'
  }

  return integerText + fractionText
}

export const getSourceTypeText = (sourceType) => {
  return sourceType === 'AI' ? 'AI识别生成' : '手工创建'
}
