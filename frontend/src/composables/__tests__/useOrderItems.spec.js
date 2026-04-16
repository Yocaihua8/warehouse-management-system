import { useOrderItems } from '../useOrderItems'

const createItem = () => ({
  productId: null,
  productCode: '',
  productName: '',
  specification: '',
  unit: '',
  quantity: 1,
  unitPrice: 0,
  remark: ''
})

describe('useOrderItems', () => {
  it('addItem 应新增一行空明细', () => {
    const form = { itemList: [createItem()] }
    const { addItem } = useOrderItems({ form, createEmptyItem: createItem })

    addItem()

    expect(form.itemList).toHaveLength(2)
  })

  it('removeItem 达到最小行数时应阻止删除并触发回调', () => {
    const form = { itemList: [createItem()] }
    const onMinItemsReached = vi.fn()
    const { removeItem } = useOrderItems({
      form,
      createEmptyItem: createItem,
      minItems: 1,
      onMinItemsReached
    })

    expect(removeItem(0)).toBe(false)
    expect(form.itemList).toHaveLength(1)
    expect(onMinItemsReached).toHaveBeenCalled()
  })

  it('insertItem 应在指定行后插入空行', () => {
    const form = {
      itemList: [
        { ...createItem(), productCode: 'A' },
        { ...createItem(), productCode: 'B' }
      ]
    }
    const { insertItem } = useOrderItems({ form, createEmptyItem: createItem })

    insertItem(0)

    expect(form.itemList).toHaveLength(3)
    expect(form.itemList[0].productCode).toBe('A')
    expect(form.itemList[1].productCode).toBe('')
    expect(form.itemList[2].productCode).toBe('B')
  })

  it('updateRowField 应更新指定行字段', () => {
    const form = { itemList: [createItem()] }
    const { updateRowField } = useOrderItems({ form, createEmptyItem: createItem })

    updateRowField(0, 'remark', '测试备注')

    expect(form.itemList[0].remark).toBe('测试备注')
  })

  it('selectProduct 应按商品选项回填商品字段', () => {
    const form = { itemList: [createItem()] }
    const { selectProduct } = useOrderItems({ form, createEmptyItem: createItem })

    selectProduct(0, 2, [{
      id: 2,
      productCode: 'P-002',
      productName: '测试商品',
      specification: '500g',
      unit: '袋'
    }])

    expect(form.itemList[0]).toMatchObject({
      productId: 2,
      productCode: 'P-002',
      productName: '测试商品',
      specification: '500g',
      unit: '袋'
    })
  })

  it('selectProduct 存在 onProductChange 时应优先走自定义回调', () => {
    const form = { itemList: [createItem()] }
    const onProductChange = vi.fn((row, productId) => {
      row.productId = productId
      row.productName = '自定义回填'
    })
    const { selectProduct } = useOrderItems({
      form,
      createEmptyItem: createItem,
      onProductChange
    })

    selectProduct(0, 9, [{ id: 9, productName: '不会直接用到' }])

    expect(onProductChange).toHaveBeenCalledWith(form.itemList[0], 9)
    expect(form.itemList[0].productName).toBe('自定义回填')
  })

  it('resetItems 应重置为一行空明细', () => {
    const form = {
      itemList: [
        { ...createItem(), productName: 'A' },
        { ...createItem(), productName: 'B' }
      ]
    }
    const { resetItems } = useOrderItems({ form, createEmptyItem: createItem })

    resetItems()

    expect(form.itemList).toHaveLength(1)
    expect(form.itemList[0].productName).toBe('')
  })
})
