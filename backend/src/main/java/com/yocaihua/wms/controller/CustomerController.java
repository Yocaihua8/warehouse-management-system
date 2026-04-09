package com.yocaihua.wms.controller;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.Result;
import com.yocaihua.wms.dto.CustomerAddDTO;
import com.yocaihua.wms.dto.CustomerUpdateDTO;
import com.yocaihua.wms.entity.Customer;
import com.yocaihua.wms.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "客户管理", description = "客户 CRUD、分页查询、Excel 导出")
@SecurityRequirement(name = "token")
@RestController
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Operation(summary = "客户分页列表")
    @GetMapping("/customer/list")
    public Result<PageResult<Customer>> getCustomerPage(
            @Parameter(description = "客户编码（模糊）") @RequestParam(required = false) String customerCode,
            @Parameter(description = "客户名称（模糊）") @RequestParam(required = false) String customerName,
            @Parameter(description = "页码，默认 1") @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "每页条数，默认 10") @RequestParam(required = false) Integer pageSize) {
        return Result.success(customerService.getCustomerPage(customerCode, customerName, pageNum, pageSize));
    }

    @Operation(summary = "导出客户 Excel")
    @GetMapping("/customer/export")
    public ResponseEntity<byte[]> exportCustomer(
            @Parameter(description = "客户编码（模糊）") @RequestParam(required = false) String customerCode,
            @Parameter(description = "客户名称（模糊）") @RequestParam(required = false) String customerName) {
        byte[] content = customerService.exportCustomerExcel(customerCode, customerName);
        String fileName = "客户列表.xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @Operation(summary = "查询客户详情")
    @GetMapping("/customer/{id}")
    public Result<Customer> getCustomerById(@Parameter(description = "客户 ID") @PathVariable Long id) {
        return Result.success(customerService.getCustomerById(id));
    }

    @Operation(summary = "新增客户", description = "【Admin】")
    @PostMapping("/customer/add")
    public Result<String> addCustomer(@RequestBody @Valid CustomerAddDTO customerAddDTO) {
        return Result.success(customerService.addCustomer(customerAddDTO));
    }

    @Operation(summary = "修改客户", description = "【Admin】")
    @PutMapping("/customer/update")
    public Result<String> updateCustomer(@RequestBody @Valid CustomerUpdateDTO customerUpdateDTO) {
        return Result.success(customerService.updateCustomer(customerUpdateDTO));
    }

    @Operation(summary = "删除客户", description = "【Admin】")
    @DeleteMapping("/customer/delete/{id}")
    public Result<String> deleteCustomer(@Parameter(description = "客户 ID") @PathVariable Long id) {
        return Result.success(customerService.deleteCustomer(id));
    }
}
