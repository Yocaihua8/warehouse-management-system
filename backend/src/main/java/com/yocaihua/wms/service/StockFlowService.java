package com.yocaihua.wms.service;

import com.yocaihua.wms.entity.InboundOrderItem;
import com.yocaihua.wms.entity.OutboundOrderItem;

import java.util.List;

public interface StockFlowService {

    void increaseByInbound(Long orderId,
                           String orderNo,
                           List<InboundOrderItem> itemList,
                           String operatorName,
                           String changeType,
                           String remark);

    void decreaseByOutbound(Long orderId,
                            String orderNo,
                            List<OutboundOrderItem> itemList,
                            String operatorName,
                            String changeType,
                            String remark);

    void rollbackInboundOnVoid(Long orderId,
                               String orderNo,
                               List<InboundOrderItem> itemList,
                               String operatorName,
                               String remark);

    void rollbackOutboundOnVoid(Long orderId,
                                String orderNo,
                                List<OutboundOrderItem> itemList,
                                String operatorName,
                                String remark);

    void recordManualAdjust(Long productId,
                            String productNameSnapshot,
                            Integer beforeQuantity,
                            Integer afterQuantity,
                            String operatorName,
                            String reason,
                            String remark);
}
