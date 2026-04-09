package com.yocaihua.wms.mapper;

import com.yocaihua.wms.vo.DashboardDailyCountRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DashboardMapper {

    Long countProduct();

    Long countCustomer();

    Long countStockProduct();

    Long countLowStock();

    Long countInboundOrder();

    Long countOutboundOrder();

    List<DashboardDailyCountRowVO> selectInboundDailyCount(@Param("startDate") LocalDate startDate);

    List<DashboardDailyCountRowVO> selectOutboundDailyCount(@Param("startDate") LocalDate startDate);
}
