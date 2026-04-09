package com.yocaihua.wms.service;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.dto.ProductAddDTO;
import com.yocaihua.wms.dto.ProductUpdateDTO;
import com.yocaihua.wms.entity.Product;

public interface ProductService {

    PageResult<Product> getProductPage(String productCode, String productName, Integer pageNum, Integer pageSize);

    byte[] exportProductExcel(String productCode, String productName);

    Product getProductById(Long id);

    String addProduct(ProductAddDTO productAddDTO);

    String updateProduct(ProductUpdateDTO productUpdateDTO);

    String deleteProduct(Long id);
}
