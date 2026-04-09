package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.StockChangeTypeConstant;
import com.yocaihua.wms.entity.InboundOrderItem;
import com.yocaihua.wms.entity.OutboundOrderItem;
import com.yocaihua.wms.entity.StockAdjustLog;
import com.yocaihua.wms.mapper.StockAdjustLogMapper;
import com.yocaihua.wms.mapper.StockMapper;
import com.yocaihua.wms.service.StockFlowService;
import com.yocaihua.wms.vo.StockVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StockFlowServiceImpl implements StockFlowService {

    private final StockMapper stockMapper;
    private final StockAdjustLogMapper stockAdjustLogMapper;

    public StockFlowServiceImpl(StockMapper stockMapper,
                                StockAdjustLogMapper stockAdjustLogMapper) {
        this.stockMapper = stockMapper;
        this.stockAdjustLogMapper = stockAdjustLogMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseByInbound(Long orderId,
                                  String orderNo,
                                  List<InboundOrderItem> itemList,
                                  String operatorName,
                                  String changeType,
                                  String remark) {
        if (itemList == null || itemList.isEmpty()) {
            return;
        }

        for (InboundOrderItem item : itemList) {
            StockVO stockVO = stockMapper.selectByProductId(item.getProductId());
            if (stockVO == null) {
                throw new BusinessException("库存记录不存在，productId=" + item.getProductId());
            }

            int beforeQuantity = stockVO.getQuantity() == null ? 0 : stockVO.getQuantity();
            int stockRows = stockMapper.increaseStock(item.getProductId(), item.getQuantity());
            if (stockRows <= 0) {
                throw new BusinessException("增加库存失败，productId=" + item.getProductId());
            }

            StockAdjustLog log = new StockAdjustLog();
            log.setProductId(item.getProductId());
            log.setProductNameSnapshot(resolveProductNameSnapshot(item, stockVO));
            log.setBeforeQuantity(beforeQuantity);
            log.setAfterQuantity(beforeQuantity + item.getQuantity());
            log.setChangeQuantity(item.getQuantity());
            log.setChangeType(changeType);
            log.setBizOrderId(orderId);
            log.setBizOrderNo(orderNo);
            log.setOperatorName(operatorName);
            log.setReason(resolveInboundReason(changeType));
            log.setRemark(remark);
            stockAdjustLogMapper.insert(log);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decreaseByOutbound(Long orderId,
                                   String orderNo,
                                   List<OutboundOrderItem> itemList,
                                   String operatorName,
                                   String changeType,
                                   String remark) {
        if (itemList == null || itemList.isEmpty()) {
            return;
        }

        for (OutboundOrderItem item : itemList) {
            StockVO stockVO = stockMapper.selectByProductId(item.getProductId());
            if (stockVO == null) {
                throw new BusinessException("库存记录不存在，productId=" + item.getProductId());
            }

            int beforeQuantity = stockVO.getQuantity() == null ? 0 : stockVO.getQuantity();
            if (beforeQuantity < item.getQuantity()) {
                throw new BusinessException(buildOutboundInsufficientStockMessage(orderNo, item, beforeQuantity));
            }

            int stockRows = stockMapper.decreaseStock(item.getProductId(), item.getQuantity());
            if (stockRows <= 0) {
                StockVO latestStock = stockMapper.selectByProductId(item.getProductId());
                int latestQuantity = latestStock == null || latestStock.getQuantity() == null ? 0 : latestStock.getQuantity();
                throw new BusinessException(buildOutboundInsufficientStockMessage(orderNo, item, latestQuantity));
            }

            StockAdjustLog log = new StockAdjustLog();
            log.setProductId(item.getProductId());
            log.setProductNameSnapshot(resolveProductNameSnapshot(item, stockVO));
            log.setBeforeQuantity(beforeQuantity);
            log.setAfterQuantity(beforeQuantity - item.getQuantity());
            log.setChangeQuantity(-item.getQuantity());
            log.setChangeType(changeType);
            log.setBizOrderId(orderId);
            log.setBizOrderNo(orderNo);
            log.setOperatorName(operatorName);
            log.setReason(resolveOutboundReason(changeType));
            log.setRemark(remark);
            stockAdjustLogMapper.insert(log);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackInboundOnVoid(Long orderId,
                                      String orderNo,
                                      List<InboundOrderItem> itemList,
                                      String operatorName,
                                      String remark) {
        if (itemList == null || itemList.isEmpty()) {
            return;
        }

        for (InboundOrderItem item : itemList) {
            StockVO stockVO = stockMapper.selectByProductId(item.getProductId());
            if (stockVO == null) {
                throw new BusinessException("库存记录不存在，productId=" + item.getProductId());
            }

            int beforeQuantity = stockVO.getQuantity() == null ? 0 : stockVO.getQuantity();
            if (beforeQuantity < item.getQuantity()) {
                throw new BusinessException("作废入库单失败，当前库存不足以回退商品，productId=" + item.getProductId());
            }

            int stockRows = stockMapper.decreaseStock(item.getProductId(), item.getQuantity());
            if (stockRows <= 0) {
                throw new BusinessException("作废入库单回退库存失败，productId=" + item.getProductId());
            }

            StockAdjustLog log = new StockAdjustLog();
            log.setProductId(item.getProductId());
            log.setProductNameSnapshot(resolveProductNameSnapshot(item, stockVO));
            log.setBeforeQuantity(beforeQuantity);
            log.setAfterQuantity(beforeQuantity - item.getQuantity());
            log.setChangeQuantity(-item.getQuantity());
            log.setChangeType(StockChangeTypeConstant.VOID_INBOUND);
            log.setBizOrderId(orderId);
            log.setBizOrderNo(orderNo);
            log.setOperatorName(operatorName);
            log.setReason(resolveVoidInboundReason());
            log.setRemark(remark);
            stockAdjustLogMapper.insert(log);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackOutboundOnVoid(Long orderId,
                                       String orderNo,
                                       List<OutboundOrderItem> itemList,
                                       String operatorName,
                                       String remark) {
        if (itemList == null || itemList.isEmpty()) {
            return;
        }

        for (OutboundOrderItem item : itemList) {
            StockVO stockVO = stockMapper.selectByProductId(item.getProductId());
            if (stockVO == null) {
                throw new BusinessException("库存记录不存在，productId=" + item.getProductId());
            }

            int beforeQuantity = stockVO.getQuantity() == null ? 0 : stockVO.getQuantity();

            int stockRows = stockMapper.increaseStock(item.getProductId(), item.getQuantity());
            if (stockRows <= 0) {
                throw new BusinessException("作废出库单回补库存失败，productId=" + item.getProductId());
            }

            StockAdjustLog log = new StockAdjustLog();
            log.setProductId(item.getProductId());
            log.setProductNameSnapshot(resolveProductNameSnapshot(item, stockVO));
            log.setBeforeQuantity(beforeQuantity);
            log.setAfterQuantity(beforeQuantity + item.getQuantity());
            log.setChangeQuantity(item.getQuantity());
            log.setChangeType(StockChangeTypeConstant.VOID_OUTBOUND);
            log.setBizOrderId(orderId);
            log.setBizOrderNo(orderNo);
            log.setOperatorName(operatorName);
            log.setReason(resolveVoidOutboundReason());
            log.setRemark(remark);
            stockAdjustLogMapper.insert(log);
        }
    }

    @Override
    public void recordManualAdjust(Long productId,
                                   String productNameSnapshot,
                                   Integer beforeQuantity,
                                   Integer afterQuantity,
                                   String operatorName,
                                   String reason,
                                   String remark) {
        StockAdjustLog log = new StockAdjustLog();
        log.setProductId(productId);
        log.setProductNameSnapshot(productNameSnapshot);
        log.setBeforeQuantity(beforeQuantity);
        log.setAfterQuantity(afterQuantity);
        log.setChangeQuantity(afterQuantity - beforeQuantity);
        log.setChangeType(StockChangeTypeConstant.MANUAL_ADJUST);
        log.setOperatorName(operatorName);
        log.setReason(reason);
        log.setRemark(remark);
        stockAdjustLogMapper.insert(log);
    }

    private String resolveProductNameSnapshot(InboundOrderItem item, StockVO stockVO) {
        if (item.getProductNameSnapshot() != null && !item.getProductNameSnapshot().isBlank()) {
            return item.getProductNameSnapshot();
        }
        return stockVO.getProductName();
    }

    private String resolveProductNameSnapshot(OutboundOrderItem item, StockVO stockVO) {
        if (item.getProductNameSnapshot() != null && !item.getProductNameSnapshot().isBlank()) {
            return item.getProductNameSnapshot();
        }
        return stockVO.getProductName();
    }

    private String resolveInboundReason(String changeType) {
        if (StockChangeTypeConstant.AI_CONFIRM_INBOUND.equals(changeType)) {
            return "AI确认入库增加库存";
        }
        return "手工入库增加库存";
    }

    private String resolveOutboundReason(String changeType) {
        if (StockChangeTypeConstant.AI_CONFIRM_OUTBOUND.equals(changeType)) {
            return "AI确认出库扣减库存";
        }
        return "手工出库扣减库存";
    }

    private String resolveVoidInboundReason() {
        return "作废已入库单据，回退库存";
    }

    private String resolveVoidOutboundReason() {
        return "作废已出库单据，回补库存";
    }

    private String buildOutboundInsufficientStockMessage(String orderNo, OutboundOrderItem item, int availableQuantity) {
        String resolvedOrderNo = (orderNo == null || orderNo.isBlank()) ? "-" : orderNo.trim();
        String productName = item.getProductNameSnapshot();
        if (productName == null || productName.isBlank()) {
            productName = "商品ID=" + item.getProductId();
        } else {
            productName = productName.trim();
        }
        return "出库单[" + resolvedOrderNo + "]确认失败：商品[" + productName + "]库存不足，需出库"
                + item.getQuantity() + "，当前可用" + availableQuantity + "。请刷新草稿后重试";
    }
}
