package com.yocaihua.wms.service;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.dto.CustomerAddDTO;
import com.yocaihua.wms.dto.CustomerUpdateDTO;
import com.yocaihua.wms.entity.Customer;

public interface CustomerService {

    PageResult<Customer> getCustomerPage(String customerCode, String customerName, Integer pageNum, Integer pageSize);

    byte[] exportCustomerExcel(String customerCode, String customerName);

    Customer getCustomerById(Long id);

    String addCustomer(CustomerAddDTO customerAddDTO);

    String updateCustomer(CustomerUpdateDTO customerUpdateDTO);

    String deleteCustomer(Long id);
}
