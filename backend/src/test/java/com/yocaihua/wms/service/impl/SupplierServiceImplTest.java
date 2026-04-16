package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.dto.SupplierAddDTO;
import com.yocaihua.wms.dto.SupplierUpdateDTO;
import com.yocaihua.wms.entity.Supplier;
import com.yocaihua.wms.mapper.InboundOrderMapper;
import com.yocaihua.wms.mapper.SupplierMapper;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierServiceImplTest {

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private InboundOrderMapper inboundOrderMapper;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    @Test
    void getSupplierPage_shouldUseDefaultPagination_whenPageParamsInvalid() {
        Supplier supplier = supplier(1L, "S001", "供应商A", 1);
        when(supplierMapper.count("S001", "供应商A")).thenReturn(1L);
        when(supplierMapper.selectPage("S001", "供应商A", 0, 10)).thenReturn(List.of(supplier));

        PageResult<Supplier> result = supplierService.getSupplierPage("S001", "供应商A", 0, 0);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getPageNum());
        assertEquals(10, result.getPageSize());
        assertEquals(1, result.getList().size());
        verify(supplierMapper).selectPage("S001", "供应商A", 0, 10);
    }

    @Test
    void getSupplierPage_shouldClampPageSizeToMax() {
        when(supplierMapper.count(null, null)).thenReturn(0L);
        when(supplierMapper.selectPage(null, null, 0, 200)).thenReturn(List.of());

        PageResult<Supplier> result = supplierService.getSupplierPage(null, null, 1, 999);

        assertEquals(200, result.getPageSize());
        verify(supplierMapper).selectPage(null, null, 0, 200);
    }

    @Test
    void getSupplierPage_shouldKeepOriginalKeywordAndOffset_whenPageIsLater() {
        Supplier supplier = supplier(6L, "S006", "供应商后页", 1);
        when(supplierMapper.count("  S006  ", "  供应商后页  ")).thenReturn(1L);
        when(supplierMapper.selectPage("  S006  ", "  供应商后页  ", 5, 5)).thenReturn(List.of(supplier));

        PageResult<Supplier> result = supplierService.getSupplierPage("  S006  ", "  供应商后页  ", 2, 5);

        assertEquals(1L, result.getTotal());
        assertEquals(2, result.getPageNum());
        assertEquals(5, result.getPageSize());
        assertEquals("供应商后页", result.getList().get(0).getSupplierName());
        verify(supplierMapper).selectPage("  S006  ", "  供应商后页  ", 5, 5);
    }

    @Test
    void getSupplierById_shouldThrow_whenMissing() {
        when(supplierMapper.selectById(10L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> supplierService.getSupplierById(10L));

        assertEquals("供应商不存在", exception.getMessage());
    }

    @Test
    void getSupplierById_shouldReturnSupplier_whenExists() {
        Supplier existing = supplier(10L, "S010", "供应商A", 1);
        when(supplierMapper.selectById(10L)).thenReturn(existing);

        Supplier result = supplierService.getSupplierById(10L);

        assertEquals(10L, result.getId());
        assertEquals("供应商A", result.getSupplierName());
    }

    @Test
    void addSupplier_shouldThrow_whenCodeExists() {
        SupplierAddDTO dto = new SupplierAddDTO();
        dto.setSupplierCode("S001");
        dto.setSupplierName("供应商A");
        when(supplierMapper.selectBySupplierCode("S001")).thenReturn(supplier(1L, "S001", "旧供应商", 1));

        BusinessException exception = assertThrows(BusinessException.class, () -> supplierService.addSupplier(dto));

        assertEquals("供应商编码已存在", exception.getMessage());
        verify(supplierMapper, never()).insert(any());
    }

    @Test
    void addSupplier_shouldThrow_whenInsertFails() {
        SupplierAddDTO dto = new SupplierAddDTO();
        dto.setSupplierCode("S001");
        dto.setSupplierName("供应商A");
        when(supplierMapper.selectBySupplierCode("S001")).thenReturn(null);
        when(supplierMapper.insert(dto)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> supplierService.addSupplier(dto));

        assertEquals("新增供应商失败", exception.getMessage());
    }

    @Test
    void addSupplier_shouldReturnSuccess_whenInserted() {
        SupplierAddDTO dto = new SupplierAddDTO();
        dto.setSupplierCode("S001");
        dto.setSupplierName("供应商A");
        when(supplierMapper.selectBySupplierCode("S001")).thenReturn(null);
        when(supplierMapper.insert(dto)).thenReturn(1);

        String result = supplierService.addSupplier(dto);

        assertEquals("新增供应商成功", result);
    }

    @Test
    void updateSupplier_shouldThrow_whenSupplierMissing() {
        SupplierUpdateDTO dto = new SupplierUpdateDTO();
        dto.setId(8L);
        dto.setSupplierCode("S008");
        dto.setSupplierName("供应商新");
        dto.setStatus(1);
        when(supplierMapper.selectById(8L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> supplierService.updateSupplier(dto));

        assertEquals("供应商不存在", exception.getMessage());
        verify(supplierMapper, never()).updateById(any());
    }

    @Test
    void updateSupplier_shouldThrow_whenDuplicateCodeExists() {
        SupplierUpdateDTO dto = new SupplierUpdateDTO();
        dto.setId(8L);
        dto.setSupplierCode("S009");
        dto.setSupplierName("供应商新");
        dto.setStatus(1);

        when(supplierMapper.selectById(8L)).thenReturn(supplier(8L, "S008", "供应商旧", 1));
        when(supplierMapper.selectBySupplierCodeExcludeId("S009", 8L)).thenReturn(supplier(9L, "S009", "供应商重复", 1));

        BusinessException exception = assertThrows(BusinessException.class, () -> supplierService.updateSupplier(dto));

        assertEquals("供应商编码已存在", exception.getMessage());
        verify(supplierMapper, never()).updateById(any());
    }

    @Test
    void updateSupplier_shouldThrow_whenUpdateFails() {
        SupplierUpdateDTO dto = new SupplierUpdateDTO();
        dto.setId(8L);
        dto.setSupplierCode("S008");
        dto.setSupplierName("供应商新");
        dto.setStatus(1);

        when(supplierMapper.selectById(8L)).thenReturn(supplier(8L, "S008", "供应商旧", 1));
        when(supplierMapper.selectBySupplierCodeExcludeId("S008", 8L)).thenReturn(null);
        when(supplierMapper.updateById(dto)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> supplierService.updateSupplier(dto));

        assertEquals("修改供应商失败", exception.getMessage());
    }

    @Test
    void updateSupplier_shouldReturnSuccess_whenUpdated() {
        SupplierUpdateDTO dto = new SupplierUpdateDTO();
        dto.setId(8L);
        dto.setSupplierCode("S008");
        dto.setSupplierName("供应商新");
        dto.setStatus(1);

        when(supplierMapper.selectById(8L)).thenReturn(supplier(8L, "S008", "供应商旧", 1));
        when(supplierMapper.selectBySupplierCodeExcludeId("S008", 8L)).thenReturn(null);
        when(supplierMapper.updateById(dto)).thenReturn(1);

        String result = supplierService.updateSupplier(dto);

        assertEquals("修改供应商成功", result);
    }

    @Test
    void deleteSupplier_shouldThrow_whenReferencedByInboundOrderSupplierId() {
        Supplier supplier = supplier(10L, "S010", "供应商A", 1);
        when(supplierMapper.selectById(10L)).thenReturn(supplier);
        when(inboundOrderMapper.countBySupplierId(10L)).thenReturn(2);
        when(inboundOrderMapper.countBySupplierNameWhenSupplierIdMissing("供应商A")).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> supplierService.deleteSupplier(10L));

        assertEquals("供应商已被入库单引用，不能删除", exception.getMessage());
        verify(supplierMapper, never()).deleteById(any());
    }

    @Test
    void deleteSupplier_shouldThrow_whenReferencedByLegacySupplierName() {
        Supplier supplier = supplier(10L, "S010", "供应商A", 1);
        when(supplierMapper.selectById(10L)).thenReturn(supplier);
        when(inboundOrderMapper.countBySupplierId(10L)).thenReturn(0);
        when(inboundOrderMapper.countBySupplierNameWhenSupplierIdMissing("供应商A")).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class, () -> supplierService.deleteSupplier(10L));

        assertEquals("供应商已被入库单引用，不能删除", exception.getMessage());
        verify(supplierMapper, never()).deleteById(any());
    }

    @Test
    void deleteSupplier_shouldThrow_whenDeleteFails() {
        Supplier supplier = supplier(10L, "S010", "供应商A", 1);
        when(supplierMapper.selectById(10L)).thenReturn(supplier);
        when(inboundOrderMapper.countBySupplierId(10L)).thenReturn(0);
        when(inboundOrderMapper.countBySupplierNameWhenSupplierIdMissing("供应商A")).thenReturn(0);
        when(supplierMapper.deleteById(10L)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> supplierService.deleteSupplier(10L));

        assertEquals("删除供应商失败", exception.getMessage());
    }

    @Test
    void deleteSupplier_shouldReturnSuccess_whenNoReference() {
        Supplier supplier = supplier(10L, "S010", "供应商A", 1);
        when(supplierMapper.selectById(10L)).thenReturn(supplier);
        when(inboundOrderMapper.countBySupplierId(10L)).thenReturn(0);
        when(inboundOrderMapper.countBySupplierNameWhenSupplierIdMissing("供应商A")).thenReturn(0);
        when(supplierMapper.deleteById(10L)).thenReturn(1);

        String result = supplierService.deleteSupplier(10L);

        assertEquals("删除供应商成功", result);
        verify(supplierMapper).deleteById(eq(10L));
    }

    @Test
    void deleteSupplier_shouldThrow_whenSupplierMissing() {
        when(supplierMapper.selectById(10L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> supplierService.deleteSupplier(10L));

        assertEquals("供应商不存在", exception.getMessage());
        verify(supplierMapper, never()).deleteById(any());
    }

    @Test
    void exportSupplierExcel_shouldWriteWorkbook() throws IOException {
        Supplier supplier = supplier(null, " S001 ", " 供应商A ", 0);
        supplier.setContactPerson(null);
        supplier.setPhone(" 010-88886666 ");
        supplier.setAddress(null);
        supplier.setRemark(" 核心供应商 ");
        supplier.setCreatedTime(LocalDateTime.of(2026, 4, 16, 11, 20, 30));
        supplier.setUpdatedTime(null);

        when(supplierMapper.selectExportList("  S001  ", "  供应商A  ")).thenReturn(List.of(supplier));

        byte[] bytes = supplierService.exportSupplierExcel("  S001  ", "  供应商A  ");

        assertTrue(bytes.length > 0);
        verify(supplierMapper).selectExportList("  S001  ", "  供应商A  ");

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals("供应商列表", workbook.getSheetAt(0).getSheetName());
            assertEquals("供应商编码", workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue());
            assertEquals("", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
            assertEquals("S001", workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
            assertEquals("供应商A", workbook.getSheetAt(0).getRow(1).getCell(2).getStringCellValue());
            assertEquals("", workbook.getSheetAt(0).getRow(1).getCell(3).getStringCellValue());
            assertEquals("010-88886666", workbook.getSheetAt(0).getRow(1).getCell(4).getStringCellValue());
            assertEquals("", workbook.getSheetAt(0).getRow(1).getCell(5).getStringCellValue());
            assertEquals("核心供应商", workbook.getSheetAt(0).getRow(1).getCell(6).getStringCellValue());
            assertEquals("停用", workbook.getSheetAt(0).getRow(1).getCell(7).getStringCellValue());
            assertEquals("2026-04-16 11:20:30", workbook.getSheetAt(0).getRow(1).getCell(8).getStringCellValue());
            assertEquals("", workbook.getSheetAt(0).getRow(1).getCell(9).getStringCellValue());
        }
    }

    private Supplier supplier(Long id, String code, String name, Integer status) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setSupplierCode(code);
        supplier.setSupplierName(name);
        supplier.setStatus(status);
        return supplier;
    }
}
