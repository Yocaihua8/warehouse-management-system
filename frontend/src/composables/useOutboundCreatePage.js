import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCustomerList } from '../api/customer'
import { getProductList } from '../api/product'
import { getStockList } from '../api/stock'
import { addOutboundOrder, confirmOutboundOrder, getOutboundDetail, updateOutboundOrderDraft } from '../api/outbound'
import { ORDER_TYPE, parsePageData, today } from '../utils/orderHelper'
import { useOrderWorkbenchPage } from './useOrderWorkbenchPage'

export function useOutboundCreatePage() {
  const customerOptions = ref([])

  const loadCustomers = async () => {
    try {
      const res = await getCustomerList({ pageNum: 1, pageSize: 200 })
      customerOptions.value = res.data?.code === 1 ? parsePageData(res.data.data).list : []
      if (res.data?.code !== 1) {
        ElMessage.error(res.data?.message || '加载客户列表失败')
      }
    } catch (error) {
      customerOptions.value = []
      ElMessage.error(error?.response?.data?.message || error?.message || '请求客户列表接口失败')
    }
  }

  const loadAvailableStock = async (row, productId, productOptions = []) => {
    if (!row || !productId) {
      row.availableStock = null
      return
    }

    try {
      const selectedProduct = productOptions.find(item => Number(item.id) === Number(productId))
      if (!selectedProduct?.productCode) {
        row.availableStock = null
        return
      }

      const res = await getStockList({
        productCode: selectedProduct.productCode,
        pageNum: 1,
        pageSize: 20
      })
      if (res.data?.code !== 1) {
        row.availableStock = null
        return
      }

      const stockList = parsePageData(res.data?.data).list
      const matched = stockList.find(item => Number(item.productId) === Number(productId))
      row.availableStock = matched?.quantity != null ? Number(matched.quantity) : 0
    } catch (error) {
      row.availableStock = null
    }
  }

  const refreshItemStocks = async (itemList = [], productOptions = []) => {
    const tasks = (itemList || [])
      .filter(item => item?.productId)
      .map(item => loadAvailableStock(item, item.productId, productOptions))
    if (tasks.length > 0) {
      await Promise.all(tasks)
    }
  }

  const workbench = useOrderWorkbenchPage({
    orderType: ORDER_TYPE.OUTBOUND,
    defaultValues: {
      orderNo: '',
      orderDate: today(),
      sourceType: 'MANUAL',
      customerId: null,
      remark: ''
    },
    initialItemCount: 8,
    fetchProductList: getProductList,
    fetchDetail: getOutboundDetail,
    saveDraftApi: addOutboundOrder,
    updateDraftApi: updateOutboundOrderDraft,
    confirmApi: confirmOutboundOrder,
    listRoute: '/outbound/list',
    detailRouteBuilder: (draftId) => `/outbound/detail/${draftId}`,
    printRouteBuilder: (draftId) => `/outbound/print/${draftId}`,
    buildPayload: (form) => ({
      customerId: form.customerId,
      remark: form.remark,
      itemList: form.itemList.map(item => ({
        productId: item.productId,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        remark: item.remark
      }))
    }),
    buildSnapshot: (form) => JSON.stringify({
      orderDate: form.orderDate || '',
      customerId: form.customerId != null ? Number(form.customerId) : null,
      remark: form.remark || '',
      itemList: (form.itemList || []).map(item => ({
        productId: item.productId != null ? Number(item.productId) : null,
        quantity: Number(item.quantity || 0),
        unitPrice: Number(item.unitPrice || 0),
        remark: item.remark || ''
      }))
    }),
    mapDetailHeader: (source, target) => {
      target.orderNo = source.orderNo || ''
      target.orderDate = (source.createdTime || '').slice(0, 10) || today()
      target.sourceType = source.sourceType || 'MANUAL'
      target.customerId = source.customerId != null ? Number(source.customerId) : null
      target.remark = source.remark || ''
    },
    loadInitialData: async () => {
      await loadCustomers()
    },
    afterDraftLoaded: async ({ form, productOptions }) => {
      await refreshItemStocks(form.itemList, productOptions)
    },
    afterProductsLoaded: async ({ form, productOptions }) => {
      await refreshItemStocks(form.itemList, productOptions)
    },
    onProductSelected: async ({ row, productId, productOptions }) => {
      await loadAvailableStock(row, productId, productOptions)
    },
    messages: {
      minItemsReached: '至少保留一条出库明细',
      loadDraftFailed: '加载出库草稿失败',
      draftNotFound: '出库草稿不存在',
      saveFailed: '保存出库草稿失败',
      updateSuccess: ({ res }) => res.data?.data || '更新出库草稿成功',
      createSuccess: ({ createdOrder }) => {
        const orderNo = createdOrder?.orderNo
        const orderId = createdOrder?.id
        if (orderNo) {
          return `保存出库单草稿成功，单号：${orderNo}`
        }
        if (orderId) {
          return `保存出库单草稿成功，ID=${orderId}`
        }
        return '保存出库单草稿成功'
      },
      confirmPermissionDenied: '仅管理员可执行确认出库',
      confirmTitle: '确认出库',
      confirmMessage: '确认后将正式出库并扣减库存，是否继续？',
      confirmSuccess: '确认出库成功',
      confirmFailed: '确认出库失败'
    },
    notify: {
      success: (message) => ElMessage.success(message),
      error: (message) => ElMessage.error(message),
      warning: (message) => ElMessage.warning(message),
      validation: (message) => ElMessage.warning(message)
    }
  })

  return {
    ...workbench,
    customerOptions
  }
}
