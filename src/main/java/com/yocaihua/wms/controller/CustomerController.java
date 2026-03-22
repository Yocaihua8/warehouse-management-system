package com.yocaihua.wms.controller;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.Result;
import com.yocaihua.wms.dto.CustomerAddDTO;
import com.yocaihua.wms.dto.CustomerUpdateDTO;
import com.yocaihua.wms.entity.Customer;
import com.yocaihua.wms.service.CustomerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/customer/list")
    public Result<PageResult<Customer>> getCustomerPage(
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        return Result.success(customerService.getCustomerPage(customerName, pageNum, pageSize));
    }

    @GetMapping("/customer/{id}")
    public Result<Customer> getCustomerById(@PathVariable Long id) {
        return Result.success(customerService.getCustomerById(id));
    }

    @PostMapping("/customer/add")
    public Result<String> addCustomer(@RequestBody CustomerAddDTO customerAddDTO) {
        return Result.success(customerService.addCustomer(customerAddDTO));
    }

    @PutMapping("/customer/update")
    public Result<String> updateCustomer(@RequestBody CustomerUpdateDTO customerUpdateDTO) {
        return Result.success(customerService.updateCustomer(customerUpdateDTO));
    }

    @DeleteMapping("/customer/delete/{id}")
    public Result<String> deleteCustomer(@PathVariable Long id) {
        return Result.success(customerService.deleteCustomer(id));
    }
}