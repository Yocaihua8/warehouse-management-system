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
                                     @Param("orderStatus") Integer orderStatus,
                                     @Param("offset") Integer offset,
                                     @Param("pageSize") Integer pageSize);

    Long count(@Param("orderNo") String orderNo,
               @Param("orderStatus") Integer orderStatus);

    OutboundOrderDetailVO selectDetailById(@Param("id") Long id);

    OutboundOrder selectById(@Param("id") Long id);

    int updateStatus(@Param("id") Long id,
                     @Param("targetStatus") Integer targetStatus,
                     @Param("sourceStatus") Integer sourceStatus);

    int updateDraftById(@Param("id") Long id,
                        @Param("customerId") Long customerId,
                        @Param("customerNameSnapshot") String customerNameSnapshot,
                        @Param("totalAmount") java.math.BigDecimal totalAmount,
                        @Param("remark") String remark,
                        @Param("sourceStatus") Integer sourceStatus);

    int countByCustomerId(@Param("customerId") Long customerId);

    int countByCustomerNameWhenCustomerIdMissing(@Param("customerName") String customerName);
}
