package com.yocaihua.wms.mapper;

import com.yocaihua.wms.entity.StockAdjustLog;
import com.yocaihua.wms.vo.StockAdjustLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockAdjustLogMapper {

    int insert(StockAdjustLog stockAdjustLog);

    Long countLog(@Param("productName") String productName);

    List<StockAdjustLogVO> selectLogPage(@Param("productName") String productName,
                                         @Param("offset") Integer offset,
                                         @Param("pageSize") Integer pageSize);
}