package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.dto.CustomerAddDTO;
import com.yocaihua.wms.dto.CustomerUpdateDTO;
import com.yocaihua.wms.entity.Customer;
import com.yocaihua.wms.mapper.CustomerMapper;
import com.yocaihua.wms.mapper.OutboundOrderMapper;
import com.yocaihua.wms.service.CustomerService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;
    private final OutboundOrderMapper outboundOrderMapper;

    public CustomerServiceImpl(CustomerMapper customerMapper,
                               OutboundOrderMapper outboundOrderMapper) {
        this.customerMapper = customerMapper;
        this.outboundOrderMapper = outboundOrderMapper;
    }

    @Override
    public PageResult<Customer> getCustomerPage(String customerName, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        int offset = (pageNum - 1) * pageSize;

        Long total = customerMapper.count(customerName);
        List<Customer> list = customerMapper.selectPage(customerName, offset, pageSize);

        return new PageResult<>(total, pageNum, pageSize, list);
    }

    @Override
    public Customer getCustomerById(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }
        return customer;
    }

    @Override
    public String addCustomer(CustomerAddDTO customerAddDTO) {
        Customer existingCustomer = customerMapper.selectByCustomerCode(customerAddDTO.getCustomerCode());
        if (existingCustomer != null) {
            throw new BusinessException("客户编码已存在");
        }

        int rows = customerMapper.insert(customerAddDTO);
        if (rows > 0) {
            return "新增客户成功";
        }
        throw new BusinessException("新增客户失败");
    }

    @Override
    public String updateCustomer(CustomerUpdateDTO customerUpdateDTO) {
        Customer existingCustomer = customerMapper.selectById(customerUpdateDTO.getId());
        if (existingCustomer == null) {
            throw new BusinessException("客户不存在");
        }

        int rows = customerMapper.updateById(customerUpdateDTO);
        if (rows > 0) {
            return "修改客户成功";
        }
        throw new BusinessException("修改客户失败");
    }

    @Override
    public String deleteCustomer(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }

        int outboundRefCount = outboundOrderMapper.countByCustomerId(id);
        if (outboundRefCount > 0) {
            throw new BusinessException("客户已被出库单引用，不能删除");
        }

        int rows = customerMapper.deleteById(id);
        if (rows > 0) {
            return "删除客户成功";
        }

        throw new BusinessException("删除客户失败");
    }
}