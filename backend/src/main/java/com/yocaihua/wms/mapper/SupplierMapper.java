package com.yocaihua.wms.mapper;

import com.yocaihua.wms.dto.SupplierAddDTO;
import com.yocaihua.wms.dto.SupplierUpdateDTO;
import com.yocaihua.wms.entity.Supplier;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SupplierMapper {

    List<Supplier> selectPage(@Param("supplierCode") String supplierCode,
                              @Param("supplierName") String supplierName,
                              @Param("offset") Integer offset,
                              @Param("pageSize") Integer pageSize);

    Long count(@Param("supplierCode") String supplierCode,
               @Param("supplierName") String supplierName);

    List<Supplier> selectExportList(@Param("supplierCode") String supplierCode,
                                    @Param("supplierName") String supplierName);

    Supplier selectById(@Param("id") Long id);

    Supplier selectBySupplierCode(@Param("supplierCode") String supplierCode);

    Supplier selectBySupplierCodeExcludeId(@Param("supplierCode") String supplierCode,
                                           @Param("excludeId") Long excludeId);

    Supplier selectByName(@Param("supplierName") String supplierName);

    List<Supplier> selectByNameLike(@Param("supplierName") String supplierName);

    int insert(SupplierAddDTO supplierAddDTO);

    int updateById(SupplierUpdateDTO supplierUpdateDTO);

    int deleteById(@Param("id") Long id);
}
