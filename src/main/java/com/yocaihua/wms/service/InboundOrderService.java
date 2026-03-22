package com.yocaihua.wms.service;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.dto.InboundOrderAddDTO;
import com.yocaihua.wms.vo.InboundOrderDetailVO;
import com.yocaihua.wms.vo.InboundOrderVO;

public interface InboundOrderService {

    String saveInboundOrder(InboundOrderAddDTO inboundOrderAddDTO);

    PageResult<InboundOrderVO> getInboundOrderPage(String orderNo, Integer pageNum, Integer pageSize);

    InboundOrderDetailVO getInboundOrderDetail(Long id);
}