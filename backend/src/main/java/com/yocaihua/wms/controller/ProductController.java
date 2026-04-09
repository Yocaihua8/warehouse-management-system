package com.yocaihua.wms.controller;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.Result;
import com.yocaihua.wms.dto.ProductAddDTO;
import com.yocaihua.wms.dto.ProductUpdateDTO;
import com.yocaihua.wms.entity.Product;
import com.yocaihua.wms.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "商品管理", description = "商品 CRUD、分页查询、Excel 导出")
@SecurityRequirement(name = "token")
@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "商品分页列表", description = "支持按商品编码、商品名称模糊筛选")
    @GetMapping("/product/list")
    public Result<PageResult<Product>> getProductPage(
            @Parameter(description = "商品编码（模糊）") @RequestParam(required = false) String productCode,
            @Parameter(description = "商品名称（模糊）") @RequestParam(required = false) String productName,
            @Parameter(description = "页码，默认 1") @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "每页条数，默认 10") @RequestParam(required = false) Integer pageSize) {
        return Result.success(productService.getProductPage(productCode, productName, pageNum, pageSize));
    }

    @Operation(summary = "导出商品 Excel", description = "支持筛选条件过滤，返回 .xlsx 文件")
    @GetMapping("/product/export")
    public ResponseEntity<byte[]> exportProduct(
            @Parameter(description = "商品编码（模糊）") @RequestParam(required = false) String productCode,
            @Parameter(description = "商品名称（模糊）") @RequestParam(required = false) String productName) {
        byte[] content = productService.exportProductExcel(productCode, productName);
        String fileName = "商品列表.xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @Operation(summary = "新增商品", description = "【Admin】新增一条商品记录，同时自动创建对应库存记录（初始库存为 0）")
    @PostMapping("/product/add")
    public Result<String> addProduct(@Valid @RequestBody ProductAddDTO productAddDTO) {
        return Result.success(productService.addProduct(productAddDTO));
    }

    @Operation(summary = "删除商品", description = "【Admin】若商品存在库存或关联订单则拒绝删除")
    @DeleteMapping("/product/delete/{id}")
    public Result<String> deleteProduct(@Parameter(description = "商品 ID") @PathVariable Long id) {
        return Result.success(productService.deleteProduct(id));
    }

    @Operation(summary = "修改商品", description = "【Admin】更新商品基本信息，不影响已生成订单的快照数据")
    @PutMapping("/product/update")
    public Result<String> updateProduct(@Valid @RequestBody ProductUpdateDTO productUpdateDTO) {
        return Result.success(productService.updateProduct(productUpdateDTO));
    }

    @Operation(summary = "查询商品详情")
    @GetMapping("/product/{id}")
    public Result<Product> getProductById(@Parameter(description = "商品 ID") @PathVariable Long id) {
        return Result.success(productService.getProductById(id));
    }
}
