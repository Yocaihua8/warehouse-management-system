package com.yocaihua.wms.service;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.dto.SupplierAddDTO;
import com.yocaihua.wms.dto.SupplierUpdateDTO;
import com.yocaihua.wms.entity.Supplier;

public interface SupplierService {

    PageResult<Supplier> getSupplierPage(String supplierCode, String supplierName, Integer pageNum, Integer pageSize);

    byte[] exportSupplierExcel(String supplierCode, String supplierName);

    Supplier getSupplierById(Long id);

    String addSupplier(SupplierAddDTO supplierAddDTO);

    String updateSupplier(SupplierUpdateDTO supplierUpdateDTO);

    String deleteSupplier(Long id);
}
