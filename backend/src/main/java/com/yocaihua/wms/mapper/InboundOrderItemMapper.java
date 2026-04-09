package com.yocaihua.wms.mapper;

import com.yocaihua.wms.entity.InboundOrderItem;
import com.yocaihua.wms.vo.InboundItemVO;
import com.yocaihua.wms.vo.InboundOrderItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InboundOrderItemMapper {

    int insert(InboundOrderItem item);

    int insertBatch(@Param("itemList") List<InboundOrderItem> itemList);

    int countByProductId(@Param("productId") Long productId);

    List<InboundOrderItemVO> selectByInboundOrderId(@Param("inboundOrderId") Long inboundOrderId);

    List<InboundOrderItem> selectEntityListByInboundOrderId(@Param("inboundOrderId") Long inboundOrderId);

    List<InboundItemVO> selectDetailItemsByInboundOrderId(Long inboundOrderId);

    int deleteByInboundOrderId(@Param("inboundOrderId") Long inboundOrderId);
}
