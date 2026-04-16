package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.dto.CustomerAddDTO;
import com.yocaihua.wms.dto.CustomerUpdateDTO;
import com.yocaihua.wms.entity.Customer;
import com.yocaihua.wms.mapper.CustomerMapper;
import com.yocaihua.wms.mapper.OutboundOrderMapper;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private OutboundOrderMapper outboundOrderMapper;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Test
    void getCustomerPage_shouldNormalizeKeywordAndClampPagination() {
        Customer customer = customer(1L, "C001", "客户A", 1);
        when(customerMapper.count("C001", "客户A")).thenReturn(1L);
        when(customerMapper.selectPage("C001", "客户A", 0, 200)).thenReturn(List.of(customer));

        PageResult<Customer> result = customerService.getCustomerPage("  C001  ", "  客户A  ", 0, 500);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getPageNum());
        assertEquals(200, result.getPageSize());
        assertEquals(1, result.getList().size());
        assertEquals("客户A", result.getList().get(0).getCustomerName());
    }

    @Test
    void getCustomerPage_shouldFallbackFilterInMemory_whenMapperReturnsEmptyButTotalPositive() {
        Customer matched = customer(1L, "C001", "客户A", 1);
        Customer unmatched = customer(2L, "C002", "客户B", 1);

        when(customerMapper.count("C001", "客户A")).thenReturn(1L);
        when(customerMapper.selectPage("C001", "客户A", 0, 10)).thenReturn(Collections.emptyList());
        when(customerMapper.selectPage(null, null, 0, 200)).thenReturn(List.of(matched, unmatched));

        PageResult<Customer> result = customerService.getCustomerPage("C001", "客户A", 1, 10);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("C001", result.getList().get(0).getCustomerCode());
        verify(customerMapper).selectPage(null, null, 0, 200);
    }

    @Test
    void getCustomerPage_shouldReturnEmpty_whenFallbackDataMissing() {
        when(customerMapper.count(null, null)).thenReturn(5L);
        when(customerMapper.selectPage(null, null, 0, 10)).thenReturn(Collections.emptyList());
        when(customerMapper.selectPage(null, null, 0, 200)).thenReturn(Collections.emptyList());

        PageResult<Customer> result = customerService.getCustomerPage(null, null, 1, 10);

        assertEquals(0L, result.getTotal());
        assertNotNull(result.getList());
        assertEquals(0, result.getList().size());
    }

    @Test
    void getCustomerById_shouldThrow_whenCustomerMissing() {
        when(customerMapper.selectById(10L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> customerService.getCustomerById(10L));

        assertEquals("客户不存在", exception.getMessage());
    }

    @Test
    void getCustomerById_shouldReturnCustomer_whenExists() {
        Customer existing = customer(10L, "C010", "客户A", 1);
        when(customerMapper.selectById(10L)).thenReturn(existing);

        Customer result = customerService.getCustomerById(10L);

        assertEquals(10L, result.getId());
        assertEquals("客户A", result.getCustomerName());
    }

    @Test
    void addCustomer_shouldDefaultStatusToEnabled_whenStatusMissing() {
        CustomerAddDTO dto = new CustomerAddDTO();
        dto.setCustomerCode("C001");
        dto.setCustomerName("客户A");

        when(customerMapper.selectByCustomerCode("C001")).thenReturn(null);
        when(customerMapper.insert(dto)).thenReturn(1);

        String result = customerService.addCustomer(dto);

        assertEquals("新增客户成功", result);
        assertEquals(1, dto.getStatus());
        verify(customerMapper).insert(dto);
    }

    @Test
    void addCustomer_shouldThrow_whenCodeExists() {
        CustomerAddDTO dto = new CustomerAddDTO();
        dto.setCustomerCode("C001");
        dto.setCustomerName("客户A");

        when(customerMapper.selectByCustomerCode("C001")).thenReturn(customer(1L, "C001", "旧客户", 1));

        BusinessException exception = assertThrows(BusinessException.class, () -> customerService.addCustomer(dto));

        assertEquals("客户编码已存在", exception.getMessage());
        verify(customerMapper, never()).insert(any());
    }

    @Test
    void addCustomer_shouldThrow_whenStatusInvalid() {
        CustomerAddDTO dto = new CustomerAddDTO();
        dto.setCustomerCode("C001");
        dto.setCustomerName("客户A");
        dto.setStatus(2);

        BusinessException exception = assertThrows(BusinessException.class, () -> customerService.addCustomer(dto));

        assertEquals("客户状态只能为启用或停用", exception.getMessage());
        verify(customerMapper, never()).selectByCustomerCode(any());
    }

    @Test
    void addCustomer_shouldThrow_whenInsertFails() {
        CustomerAddDTO dto = new CustomerAddDTO();
        dto.setCustomerCode("C001");
        dto.setCustomerName("客户A");
        dto.setStatus(1);

        when(customerMapper.selectByCustomerCode("C001")).thenReturn(null);
        when(customerMapper.insert(dto)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> customerService.addCustomer(dto));

        assertEquals("新增客户失败", exception.getMessage());
    }

    @Test
    void updateCustomer_shouldInheritExistingStatus_whenStatusMissing() {
        Customer existing = customer(8L, "C008", "客户旧", 0);

        CustomerUpdateDTO dto = new CustomerUpdateDTO();
        dto.setId(8L);
        dto.setCustomerCode("C008");
        dto.setCustomerName("客户新");

        when(customerMapper.selectById(8L)).thenReturn(existing);
        when(customerMapper.selectByCustomerCode("C008")).thenReturn(existing);
        when(customerMapper.updateById(dto)).thenReturn(1);

        String result = customerService.updateCustomer(dto);

        assertEquals("修改客户成功", result);
        assertEquals(0, dto.getStatus());
        verify(customerMapper).updateById(dto);
    }

    @Test
    void updateCustomer_shouldThrow_whenDuplicateCodeBelongsToAnotherCustomer() {
        Customer existing = customer(8L, "C008", "客户旧", 1);
        Customer duplicate = customer(9L, "C009", "客户重复", 1);

        CustomerUpdateDTO dto = new CustomerUpdateDTO();
        dto.setId(8L);
        dto.setCustomerCode("C009");
        dto.setCustomerName("客户新");

        when(customerMapper.selectById(8L)).thenReturn(existing);
        when(customerMapper.selectByCustomerCode("C009")).thenReturn(duplicate);

        BusinessException exception = assertThrows(BusinessException.class, () -> customerService.updateCustomer(dto));

        assertEquals("客户编码已存在", exception.getMessage());
        verify(customerMapper, never()).updateById(any());
    }

    @Test
    void updateCustomer_shouldThrow_whenCustomerMissing() {
        CustomerUpdateDTO dto = new CustomerUpdateDTO();
        dto.setId(8L);
        dto.setCustomerCode("C008");
        dto.setCustomerName("客户新");

        when(customerMapper.selectById(8L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> customerService.updateCustomer(dto));

        assertEquals("客户不存在", exception.getMessage());
        verify(customerMapper, never()).updateById(any());
    }

    @Test
    void updateCustomer_shouldThrow_whenStatusInvalid() {
        Customer existing = customer(8L, "C008", "客户旧", 1);

        CustomerUpdateDTO dto = new CustomerUpdateDTO();
        dto.setId(8L);
        dto.setCustomerCode("C008");
        dto.setCustomerName("客户新");
        dto.setStatus(3);

        when(customerMapper.selectById(8L)).thenReturn(existing);
        when(customerMapper.selectByCustomerCode("C008")).thenReturn(existing);

        BusinessException exception = assertThrows(BusinessException.class, () -> customerService.updateCustomer(dto));

        assertEquals("客户状态只能为启用或停用", exception.getMessage());
        verify(customerMapper, never()).updateById(any());
    }

    @Test
    void updateCustomer_shouldThrow_whenUpdateFails() {
        Customer existing = customer(8L, "C008", "客户旧", 1);

        CustomerUpdateDTO dto = new CustomerUpdateDTO();
        dto.setId(8L);
        dto.setCustomerCode("C008");
        dto.setCustomerName("客户新");
        dto.setStatus(1);

        when(customerMapper.selectById(8L)).thenReturn(existing);
        when(customerMapper.selectByCustomerCode("C008")).thenReturn(existing);
        when(customerMapper.updateById(dto)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> customerService.updateCustomer(dto));

        assertEquals("修改客户失败", exception.getMessage());
    }

    @Test
    void deleteCustomer_shouldThrow_whenReferencedByOutboundOrderCustomerId() {
        Customer customer = customer(10L, "C010", "客户A", 1);
        when(customerMapper.selectById(10L)).thenReturn(customer);
        when(outboundOrderMapper.countByCustomerId(10L)).thenReturn(2);
        when(outboundOrderMapper.countByCustomerNameWhenCustomerIdMissing("客户A")).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> customerService.deleteCustomer(10L));

        assertEquals("客户已被出库单引用，不能删除", exception.getMessage());
        verify(customerMapper, never()).deleteById(any());
    }

    @Test
    void deleteCustomer_shouldThrow_whenReferencedByLegacyCustomerName() {
        Customer customer = customer(10L, "C010", "客户A", 1);
        when(customerMapper.selectById(10L)).thenReturn(customer);
        when(outboundOrderMapper.countByCustomerId(10L)).thenReturn(0);
        when(outboundOrderMapper.countByCustomerNameWhenCustomerIdMissing("客户A")).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class, () -> customerService.deleteCustomer(10L));

        assertEquals("客户已被出库单引用，不能删除", exception.getMessage());
        verify(customerMapper, never()).deleteById(any());
    }

    @Test
    void deleteCustomer_shouldThrow_whenCustomerMissing() {
        when(customerMapper.selectById(10L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> customerService.deleteCustomer(10L));

        assertEquals("客户不存在", exception.getMessage());
        verify(customerMapper, never()).deleteById(any());
    }

    @Test
    void deleteCustomer_shouldThrow_whenDeleteFails() {
        Customer customer = customer(10L, "C010", "客户A", 1);
        when(customerMapper.selectById(10L)).thenReturn(customer);
        when(outboundOrderMapper.countByCustomerId(10L)).thenReturn(0);
        when(outboundOrderMapper.countByCustomerNameWhenCustomerIdMissing("客户A")).thenReturn(0);
        when(customerMapper.deleteById(10L)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> customerService.deleteCustomer(10L));

        assertEquals("删除客户失败", exception.getMessage());
    }

    @Test
    void deleteCustomer_shouldDelete_whenNoReference() {
        Customer customer = customer(10L, "C010", "客户A", 1);
        when(customerMapper.selectById(10L)).thenReturn(customer);
        when(outboundOrderMapper.countByCustomerId(10L)).thenReturn(0);
        when(outboundOrderMapper.countByCustomerNameWhenCustomerIdMissing("客户A")).thenReturn(0);
        when(customerMapper.deleteById(10L)).thenReturn(1);

        String result = customerService.deleteCustomer(10L);

        assertEquals("删除客户成功", result);
        verify(customerMapper).deleteById(eq(10L));
    }

    @Test
    void exportCustomerExcel_shouldNormalizeKeywordAndWriteWorkbook() throws IOException {
        Customer customer = customer(null, " C001 ", " 客户A ", 0);
        customer.setContactPerson(null);
        customer.setPhone(" 13800138000 ");
        customer.setAddress(null);
        customer.setRemark(" 重要客户 ");
        customer.setCreatedTime(LocalDateTime.of(2026, 4, 16, 10, 15, 30));
        customer.setUpdatedTime(null);

        when(customerMapper.selectExportList("C001", "客户A")).thenReturn(List.of(customer));

        byte[] bytes = customerService.exportCustomerExcel("  C001  ", "  客户A  ");

        assertTrue(bytes.length > 0);
        verify(customerMapper).selectExportList("C001", "客户A");

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals("客户列表", workbook.getSheetAt(0).getSheetName());
            assertEquals("客户编码", workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue());
            assertEquals("", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
            assertEquals("C001", workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
            assertEquals("客户A", workbook.getSheetAt(0).getRow(1).getCell(2).getStringCellValue());
            assertEquals("", workbook.getSheetAt(0).getRow(1).getCell(3).getStringCellValue());
            assertEquals("13800138000", workbook.getSheetAt(0).getRow(1).getCell(4).getStringCellValue());
            assertEquals("", workbook.getSheetAt(0).getRow(1).getCell(5).getStringCellValue());
            assertEquals("重要客户", workbook.getSheetAt(0).getRow(1).getCell(6).getStringCellValue());
            assertEquals("停用", workbook.getSheetAt(0).getRow(1).getCell(7).getStringCellValue());
            assertEquals("2026-04-16 10:15:30", workbook.getSheetAt(0).getRow(1).getCell(8).getStringCellValue());
            assertEquals("", workbook.getSheetAt(0).getRow(1).getCell(9).getStringCellValue());
        }
    }

    private Customer customer(Long id, String code, String name, Integer status) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setCustomerCode(code);
        customer.setCustomerName(name);
        customer.setStatus(status);
        return customer;
    }
}
