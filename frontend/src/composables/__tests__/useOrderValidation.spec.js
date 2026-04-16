import { ORDER_TYPE } from '../../utils/orderHelper'
import { useOrderValidation } from '../useOrderValidation'

describe('useOrderValidation', () => {
  it('入库单缺供应商时应校验失败并提示', () => {
    const notify = vi.fn()
    const { validateInboundForm } = useOrderValidation(notify)

    expect(validateInboundForm({
      supplierName: '   ',
      itemList: [{ productId: 1, quantity: 1, unitPrice: 1 }]
    })).toBe(false)
    expect(notify).toHaveBeenCalledWith('供应商名称不能为空')
  })

  it('入库单商品为空时应定位到具体行', () => {
    const notify = vi.fn()
    const { validateInboundForm } = useOrderValidation(notify)

    expect(validateInboundForm({
      supplierName: '供应商A',
      itemList: [{ productId: null, quantity: 1, unitPrice: 1 }]
    })).toBe(false)
    expect(notify).toHaveBeenCalledWith('第 1 行商品不能为空')
  })

  it('有效入库单应通过校验', () => {
    const notify = vi.fn()
    const { validateInboundForm } = useOrderValidation(notify)

    expect(validateInboundForm({
      supplierName: '供应商A',
      itemList: [{ productId: 1, quantity: 2, unitPrice: 3.5 }]
    })).toBe(true)
    expect(notify).not.toHaveBeenCalled()
  })

  it('出库单缺客户时应校验失败', () => {
    const notify = vi.fn()
    const { validateOutboundForm } = useOrderValidation(notify)

    expect(validateOutboundForm({
      customerId: null,
      itemList: [{ productId: 1, quantity: 1, unitPrice: 1 }]
    })).toBe(false)
    expect(notify).toHaveBeenCalledWith('请选择客户')
  })

  it('validateOrderForm 应按订单类型分发到出库校验', () => {
    const notify = vi.fn()
    const { validateOrderForm } = useOrderValidation(notify)

    expect(validateOrderForm(ORDER_TYPE.OUTBOUND, {
      customerId: 100,
      itemList: [{ productId: 1, quantity: 1, unitPrice: 8 }]
    })).toBe(true)
    expect(notify).not.toHaveBeenCalled()
  })

  it('AI 导入行未匹配商品时应阻止提交', () => {
    const notify = vi.fn()
    const { validateAiImportedItems } = useOrderValidation(notify)

    expect(validateAiImportedItems([
      { productId: 1 },
      { matchedProductId: null, productId: null }
    ])).toBe(false)
    expect(notify).toHaveBeenCalledWith('第 2 行商品尚未匹配，请先处理后再提交')
  })

  it('AI 导入行存在 matchedProductId 时应允许通过', () => {
    const notify = vi.fn()
    const { validateAiImportedItems } = useOrderValidation(notify)

    expect(validateAiImportedItems([
      { matchedProductId: 1001 }
    ])).toBe(true)
    expect(notify).not.toHaveBeenCalled()
  })
})
