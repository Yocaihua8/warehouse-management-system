package com.yocaihua.wms.mapper;

import com.yocaihua.wms.entity.InboundOrderItem;
import com.yocaihua.wms.vo.InboundOrderItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InboundOrderItemMapper {

    int insert(InboundOrderItem inboundOrderItem);

    List<InboundOrderItemVO> selectByInboundOrderId(@Param("inboundOrderId") Long inboundOrderId);

    int countByProductId(@Param("productId") Long productId);
}