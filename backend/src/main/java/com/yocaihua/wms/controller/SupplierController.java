package com.yocaihua.wms.controller;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.Result;
import com.yocaihua.wms.dto.SupplierAddDTO;
import com.yocaihua.wms.dto.SupplierUpdateDTO;
import com.yocaihua.wms.entity.Supplier;
import com.yocaihua.wms.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "供应商管理", description = "供应商 CRUD、分页查询、Excel 导出")
@SecurityRequirement(name = "token")
@RestController
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @Operation(summary = "供应商分页列表")
    @GetMapping("/supplier/list")
    public Result<PageResult<Supplier>> getSupplierPage(
            @Parameter(description = "供应商编码（模糊）") @RequestParam(required = false) String supplierCode,
            @Parameter(description = "供应商名称（模糊）") @RequestParam(required = false) String supplierName,
            @Parameter(description = "页码，默认 1") @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "每页条数，默认 10") @RequestParam(required = false) Integer pageSize) {
        return Result.success(supplierService.getSupplierPage(supplierCode, supplierName, pageNum, pageSize));
    }

    @Operation(summary = "导出供应商 Excel")
    @GetMapping("/supplier/export")
    public ResponseEntity<byte[]> exportSupplier(
            @Parameter(description = "供应商编码（模糊）") @RequestParam(required = false) String supplierCode,
            @Parameter(description = "供应商名称（模糊）") @RequestParam(required = false) String supplierName) {
        byte[] content = supplierService.exportSupplierExcel(supplierCode, supplierName);
        String fileName = "供应商列表.xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @Operation(summary = "查询供应商详情")
    @GetMapping("/supplier/{id}")
    public Result<Supplier> getSupplierById(@Parameter(description = "供应商 ID") @PathVariable Long id) {
        return Result.success(supplierService.getSupplierById(id));
    }

    @Operation(summary = "新增供应商", description = "【Admin】")
    @PostMapping("/supplier/add")
    public Result<String> addSupplier(@RequestBody @Valid SupplierAddDTO supplierAddDTO) {
        return Result.success(supplierService.addSupplier(supplierAddDTO));
    }

    @Operation(summary = "修改供应商", description = "【Admin】")
    @PutMapping("/supplier/update")
    public Result<String> updateSupplier(@RequestBody @Valid SupplierUpdateDTO supplierUpdateDTO) {
        return Result.success(supplierService.updateSupplier(supplierUpdateDTO));
    }

    @Operation(summary = "删除供应商", description = "【Admin】")
    @DeleteMapping("/supplier/delete/{id}")
    public Result<String> deleteSupplier(@Parameter(description = "供应商 ID") @PathVariable Long id) {
        return Result.success(supplierService.deleteSupplier(id));
    }
}
