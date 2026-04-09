package com.yocaihua.wms.service;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.dto.InboundOrderAddDTO;
import com.yocaihua.wms.vo.InboundDetailVO;
import com.yocaihua.wms.vo.InboundOrderDetailVO;
import com.yocaihua.wms.vo.InboundOrderVO;
import com.yocaihua.wms.vo.OrderCreatedVO;

public interface InboundOrderService {

    OrderCreatedVO saveInboundOrder(InboundOrderAddDTO inboundOrderAddDTO);

    String updateInboundOrderDraft(Long id, InboundOrderAddDTO inboundOrderAddDTO);

    String confirmInboundOrder(Long id);

    String voidInboundOrder(Long id, String voidReason);

    PageResult<InboundOrderVO> getInboundOrderPage(String orderNo, String sourceType, Integer orderStatus, Integer pageNum, Integer pageSize);

    InboundOrderDetailVO getInboundOrderDetail(Long id);

    InboundDetailVO getDetail(Long id);

    byte[] exportInboundOrderExcel(Long id);

    byte[] exportInboundOrderPdf(Long id);
}
