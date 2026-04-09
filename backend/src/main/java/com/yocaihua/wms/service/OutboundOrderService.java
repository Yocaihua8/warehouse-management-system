package com.yocaihua.wms.service;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.dto.OutboundOrderAddDTO;
import com.yocaihua.wms.vo.OrderCreatedVO;
import com.yocaihua.wms.vo.OutboundOrderDetailVO;
import com.yocaihua.wms.vo.OutboundOrderVO;

public interface OutboundOrderService {

    OrderCreatedVO saveOutboundOrder(OutboundOrderAddDTO outboundOrderAddDTO);

    String updateOutboundOrderDraft(Long id, OutboundOrderAddDTO outboundOrderAddDTO);

    String confirmOutboundOrder(Long id);

    String voidOutboundOrder(Long id, String voidReason);

    PageResult<OutboundOrderVO> getOutboundOrderPage(String orderNo, Integer orderStatus, Integer pageNum, Integer pageSize);

    OutboundOrderDetailVO getOutboundOrderDetail(Long id);

    byte[] exportOutboundOrderExcel(Long id);

    byte[] exportOutboundOrderPdf(Long id);
}
