package com.yocaihua.wms.controller;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.Result;
import com.yocaihua.wms.dto.ProductAddDTO;
import com.yocaihua.wms.dto.ProductUpdateDTO;
import com.yocaihua.wms.entity.Product;
import com.yocaihua.wms.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/product/list")
    public Result<PageResult<Product>> getProductPage(
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        return Result.success(productService.getProductPage(productName, pageNum, pageSize));
    }

    @PostMapping("/product/add")
    public Result<String> addProduct(@Valid @RequestBody ProductAddDTO productAddDTO) {
        return Result.success(productService.addProduct(productAddDTO));
    }

    @DeleteMapping("/product/delete/{id}")
    public Result<String> deleteProduct(@PathVariable Long id) {
        return Result.success(productService.deleteProduct(id));
    }

    @PutMapping("/product/update")
    public Result<String> updateProduct(@Valid @RequestBody ProductUpdateDTO productUpdateDTO) {
        return Result.success(productService.updateProduct(productUpdateDTO));
    }

    @GetMapping("/product/{id}")
    public Result<Product> getProductById(@PathVariable Long id) {
        return Result.success(productService.getProductById(id));
    }
}