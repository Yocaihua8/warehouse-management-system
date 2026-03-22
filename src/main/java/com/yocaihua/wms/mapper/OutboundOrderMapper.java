package com.yocaihua.wms.mapper;

import com.yocaihua.wms.entity.OutboundOrder;
import com.yocaihua.wms.vo.OutboundOrderDetailVO;
import com.yocaihua.wms.vo.OutboundOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OutboundOrderMapper {

    int insert(OutboundOrder outboundOrder);

    List<OutboundOrderVO> selectPage(@Param("orderNo") String orderNo,
                                     @Param("offset") Integer offset,
                                     @Param("pageSize") Integer pageSize);

    Long count(@Param("orderNo") String orderNo);

    OutboundOrderDetailVO selectDetailById(@Param("id") Long id);

    int countByCustomerId(@Param("customerId") Long customerId);
}