package com.yocaihua.wms.service;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.dto.OutboundOrderAddDTO;
import com.yocaihua.wms.vo.OutboundOrderDetailVO;
import com.yocaihua.wms.vo.OutboundOrderVO;

public interface OutboundOrderService {

    String saveOutboundOrder(OutboundOrderAddDTO outboundOrderAddDTO);

    PageResult<OutboundOrderVO> getOutboundOrderPage(String orderNo, Integer pageNum, Integer pageSize);

    OutboundOrderDetailVO getOutboundOrderDetail(Long id);
}