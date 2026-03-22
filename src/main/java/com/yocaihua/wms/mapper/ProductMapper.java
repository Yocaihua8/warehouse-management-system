package com.yocaihua.wms.mapper;

import com.yocaihua.wms.dto.ProductAddDTO;
import com.yocaihua.wms.dto.ProductUpdateDTO;
import com.yocaihua.wms.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    List<Product> selectPage(@Param("productName") String productName,
                             @Param("offset") Integer offset,
                             @Param("pageSize") Integer pageSize);

    Long count(@Param("productName") String productName);

    Product selectById(@Param("id") Long id);

    Product selectByProductCode(@Param("productCode") String productCode);

    int insert(ProductAddDTO productAddDTO);

    int updateById(ProductUpdateDTO productUpdateDTO);

    int deleteById(@Param("id") Long id);
}