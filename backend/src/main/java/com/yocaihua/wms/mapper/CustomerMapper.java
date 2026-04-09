package com.yocaihua.wms.mapper;

import com.yocaihua.wms.dto.CustomerAddDTO;
import com.yocaihua.wms.dto.CustomerUpdateDTO;
import com.yocaihua.wms.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CustomerMapper {

    List<Customer> selectPage(@Param("customerCode") String customerCode,
                              @Param("customerName") String customerName,
                              @Param("offset") Integer offset,
                              @Param("pageSize") Integer pageSize);

    Long count(@Param("customerCode") String customerCode,
               @Param("customerName") String customerName);

    List<Customer> selectExportList(@Param("customerCode") String customerCode,
                                    @Param("customerName") String customerName);

    Customer selectById(@Param("id") Long id);

    Customer selectByCustomerCode(@Param("customerCode") String customerCode);

    Customer selectByName(@Param("customerName") String customerName);

    List<Customer> selectByNameLike(@Param("customerName") String customerName);

    int insert(CustomerAddDTO customerAddDTO);

    int updateById(CustomerUpdateDTO customerUpdateDTO);

    int deleteById(@Param("id") Long id);
}
