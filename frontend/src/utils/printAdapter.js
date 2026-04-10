import { padPrintItems, toChineseAmount } from './printUtils'

export const displayCell = (value) => {
  if (value === null || value === undefined || value === '') {
    return '\u00A0'
  }
  return value
}

export const getSourceTypeText = (sourceType) => (sourceType === 'AI' ? 'AI识别生成' : '手工创建')

export const buildPrintItems = (list, minRows = 12) => {
  const rows = padPrintItems(list)
  if (rows.length >= minRows) {
    return rows
  }
  const extraRows = [...rows]
  while (extraRows.length < minRows) {
    extraRows.push({
      index: extraRows.length + 1,
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
  return extraRows
}

const createBasePrintData = (detail, options) => ({
  title: options.title,
  warehouseLabel: options.warehouseLabel,
  warehouseName: options.warehouseName || '总仓库',
  partyLabel: options.partyLabel,
  partyName: options.partyName || '-',
  orderNo: detail?.orderNo || '-',
  createdTime: detail?.createdTime || '-',
  remark: detail?.remark || '-',
  sourceTypeText: getSourceTypeText(detail?.sourceType),
  aiRecordId: detail?.sourceType === 'AI' ? (detail?.aiRecordId ?? '-') : '-',
  aiTaskNo: detail?.aiTaskNo || '-',
  aiSourceFileName: detail?.aiSourceFileName || '-',
  showSourceMeta: Boolean(options.showSourceMeta),
  items: buildPrintItems(detail?.itemList, options.minRows || 12),
  totalAmount: detail?.totalAmount ?? '-',
  totalAmountChinese: toChineseAmount(detail?.totalAmount)
})

export const buildInboundPrintData = (detail, options = {}) => {
  return createBasePrintData(detail, {
    title: '采购入库单',
    warehouseLabel: '入库仓库',
    partyLabel: '供货单位',
    partyName: detail?.supplierName || '-',
    showSourceMeta: true,
    minRows: 12,
    ...options
  })
}

export const buildOutboundPrintData = (detail, options = {}) => {
  return createBasePrintData(detail, {
    title: '销售出库单',
    warehouseLabel: '发货仓库',
    partyLabel: '购货单位',
    partyName: detail?.customerName || '-',
    showSourceMeta: false,
    minRows: 12,
    ...options
  })
}

export { toChineseAmount }
