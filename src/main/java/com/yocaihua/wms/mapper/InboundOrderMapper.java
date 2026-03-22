package com.yocaihua.wms.mapper;

import com.yocaihua.wms.entity.InboundOrder;
import com.yocaihua.wms.vo.InboundOrderDetailVO;
import com.yocaihua.wms.vo.InboundOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InboundOrderMapper {

    int insert(InboundOrder inboundOrder);

    List<InboundOrderVO> selectPage(@Param("orderNo") String orderNo,
                                    @Param("offset") Integer offset,
                                    @Param("pageSize") Integer pageSize);

    Long count(@Param("orderNo") String orderNo);

    InboundOrderDetailVO selectDetailById(@Param("id") Long id);
}