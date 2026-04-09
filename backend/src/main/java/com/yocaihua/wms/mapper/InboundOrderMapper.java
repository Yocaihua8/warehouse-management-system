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
                                    @Param("sourceType") String sourceType,
                                    @Param("orderStatus") Integer orderStatus,
                                    @Param("offset") Integer offset,
                                    @Param("pageSize") Integer pageSize);

    Long count(@Param("orderNo") String orderNo,
               @Param("sourceType") String sourceType,
               @Param("orderStatus") Integer orderStatus);

    InboundOrderDetailVO selectDetailById(@Param("id") Long id);

    InboundOrder selectById(Long id);

    int countBySupplierId(@Param("supplierId") Long supplierId);

    int countBySupplierNameWhenSupplierIdMissing(@Param("supplierName") String supplierName);

    int updateStatus(@Param("id") Long id,
                     @Param("targetStatus") Integer targetStatus,
                     @Param("sourceStatus") Integer sourceStatus);

    int updateDraftById(@Param("id") Long id,
                        @Param("supplierId") Long supplierId,
                        @Param("supplierName") String supplierName,
                        @Param("totalAmount") java.math.BigDecimal totalAmount,
                        @Param("remark") String remark,
                        @Param("sourceStatus") Integer sourceStatus);
}
