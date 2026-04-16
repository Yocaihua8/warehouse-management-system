import { ElMessage } from 'element-plus'
import { confirmInboundOrder, getInboundOrderDetail, saveInboundOrder, updateInboundOrderDraft } from '../api/inbound'
import { getProductList } from '../api/product'
import { ORDER_TYPE, today } from '../utils/orderHelper'
import { useOrderWorkbenchPage } from './useOrderWorkbenchPage'

export function useInboundCreatePage() {
  return useOrderWorkbenchPage({
    orderType: ORDER_TYPE.INBOUND,
    defaultValues: {
      orderNo: '',
      orderDate: today(),
      sourceType: 'MANUAL',
      supplierName: '',
      remark: ''
    },
    initialItemCount: 8,
    fetchProductList: getProductList,
    fetchDetail: getInboundOrderDetail,
    saveDraftApi: saveInboundOrder,
    updateDraftApi: updateInboundOrderDraft,
    confirmApi: confirmInboundOrder,
    listRoute: '/inbound/list',
    detailRouteBuilder: (draftId) => `/inbound/detail/${draftId}`,
    printRouteBuilder: (draftId) => `/inbound/print/${draftId}`,
    buildPayload: (form) => ({
      supplierName: form.supplierName.trim(),
      remark: form.remark,
      itemList: form.itemList.map(item => ({
        productId: Number(item.productId),
        quantity: Number(item.quantity),
        unitPrice: Number(item.unitPrice),
        remark: item.remark
      }))
    }),
    buildSnapshot: (form) => JSON.stringify({
      orderDate: form.orderDate || '',
      supplierName: (form.supplierName || '').trim(),
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
      target.supplierName = source.supplierName || ''
      target.remark = source.remark || ''
    },
    messages: {
      minItemsReached: '至少保留一条入库明细',
      loadDraftFailed: '加载入库草稿失败',
      draftNotFound: '入库草稿不存在',
      saveFailed: '保存失败',
      updateSuccess: ({ res }) => res.data?.data || '更新草稿成功',
      createSuccess: ({ createdOrder }) => {
        const orderNo = createdOrder?.orderNo
        const orderId = createdOrder?.id
        if (orderNo) {
          return `保存草稿成功，单号：${orderNo}`
        }
        if (orderId) {
          return `保存草稿成功，ID=${orderId}`
        }
        return '保存草稿成功'
      },
      confirmPermissionDenied: '仅管理员可执行确认入库',
      confirmTitle: '确认入库',
      confirmMessage: '确认后将正式入库并增加库存，是否继续？',
      confirmSuccess: '确认入库成功',
      confirmFailed: '确认入库失败'
    },
    notify: {
      success: (message) => ElMessage.success(message),
      error: (message) => ElMessage.error(message),
      warning: (message) => ElMessage.warning(message),
      validation: (message) => ElMessage.error(message)
    }
  })
}
