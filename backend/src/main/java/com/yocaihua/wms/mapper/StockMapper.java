package com.yocaihua.wms.mapper;

import com.yocaihua.wms.dto.StockUpdateDTO;
import com.yocaihua.wms.vo.StockVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockMapper {

    Long countStock(@Param("productCode") String productCode,
                    @Param("productName") String productName);

    List<StockVO> selectStockPage(@Param("productCode") String productCode,
                                  @Param("productName") String productName,
                                  @Param("offset") Integer offset,
                                  @Param("pageSize") Integer pageSize);

    List<StockVO> selectStockList(@Param("productCode") String productCode,
                                  @Param("productName") String productName);

    StockVO selectByProductId(@Param("productId") Long productId);

    int updateByProductId(StockUpdateDTO stockUpdateDTO);

    int decreaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    int increaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    int insertInitStock(@Param("productId") Long productId,
                        @Param("quantity") Integer quantity,
                        @Param("warningQuantity") Integer warningQuantity);

    int increaseQuantity(@Param("productId") Long productId,
                         @Param("quantity") Integer quantity);

    int deleteByProductId(@Param("productId") Long productId);
}
