package com.yocaihua.wms.mapper;

import com.yocaihua.wms.entity.OutboundOrderItem;
import com.yocaihua.wms.vo.OutboundOrderItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OutboundOrderItemMapper {

    int insert(OutboundOrderItem outboundOrderItem);

    List<OutboundOrderItemVO> selectByOutboundOrderId(@Param("outboundOrderId") Long outboundOrderId);

    int countByProductId(@Param("productId") Long productId);
}