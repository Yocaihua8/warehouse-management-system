import { ref } from 'vue'
import { useOrderCalc } from '../useOrderCalc'

describe('useOrderCalc', () => {
  it('calcAmount 应按数量和单价计算两位小数金额', () => {
    const { calcAmount } = useOrderCalc()

    expect(calcAmount({ quantity: 3, unitPrice: 12.5 })).toBe('37.50')
  })

  it('calcAmount 对空值应返回 0.00', () => {
    const { calcAmount } = useOrderCalc()

    expect(calcAmount({ quantity: null, unitPrice: undefined })).toBe('0.00')
  })

  it('calcTotals 应返回合计数量和合计金额', () => {
    const { calcTotals } = useOrderCalc()

    expect(calcTotals([
      { quantity: 2, unitPrice: 3.5 },
      { quantity: 1, unitPrice: 4 },
      { quantity: 3, unitPrice: 0 }
    ])).toEqual({
      totalQuantity: 6,
      totalAmount: '11.00'
    })
  })

  it('useComputedTotals 应跟随 ref 数据变化更新', () => {
    const { useComputedTotals } = useOrderCalc()
    const items = ref([
      { quantity: 1, unitPrice: 5 }
    ])

    const totals = useComputedTotals(items)
    expect(totals.totalQuantity.value).toBe(1)
    expect(totals.totalAmount.value).toBe('5.00')

    items.value = [
      { quantity: 2, unitPrice: 5 },
      { quantity: 1, unitPrice: 1.5 }
    ]

    expect(totals.totalQuantity.value).toBe(3)
    expect(totals.totalAmount.value).toBe('11.50')
  })
})
