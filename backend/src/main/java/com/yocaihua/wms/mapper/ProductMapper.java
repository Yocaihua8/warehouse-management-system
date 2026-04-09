package com.yocaihua.wms.mapper;

import com.yocaihua.wms.dto.ProductAddDTO;
import com.yocaihua.wms.dto.ProductUpdateDTO;
import com.yocaihua.wms.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    List<Product> selectPage(@Param("productCode") String productCode,
                             @Param("productName") String productName,
                             @Param("offset") Integer offset,
                             @Param("pageSize") Integer pageSize);

    Long count(@Param("productCode") String productCode,
               @Param("productName") String productName);

    List<Product> selectExportList(@Param("productCode") String productCode,
                                   @Param("productName") String productName);

    Product selectById(@Param("id") Long id);

    Product selectByProductCode(@Param("productCode") String productCode);

    Product selectByNameSpecUnit(@Param("productName") String productName,
                                 @Param("specification") String specification,
                                 @Param("unit") String unit);

    Product selectByNameSpec(@Param("productName") String productName,
                             @Param("specification") String specification);

    Product selectByName(@Param("productName") String productName);

    List<Product> selectByNameLike(@Param("productName") String productName);

    List<Product> selectAiCandidates(@Param("limit") Integer limit);

    int insert(ProductAddDTO productAddDTO);

    int updateById(ProductUpdateDTO productUpdateDTO);

    int deleteById(@Param("id") Long id);


}
