package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.CurrentUserContext;
import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.dto.StockUpdateDTO;
import com.yocaihua.wms.mapper.StockMapper;
import com.yocaihua.wms.service.StockFlowService;
import com.yocaihua.wms.vo.StockVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock
    private StockMapper stockMapper;

    @Mock
    private StockFlowService stockFlowService;

    @InjectMocks
    private StockServiceImpl stockService;

    @BeforeEach
    void setUp() {
        CurrentUserContext.clear();
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void getStockPage_shouldUseDefaultPagination_whenPageParamsInvalid() {
        StockVO stock = stock(1L, "P001", "商品A", 12, 5);
        when(stockMapper.countStock("P001", "商品A")).thenReturn(1L);
        when(stockMapper.selectStockPage("P001", "商品A", 0, 10)).thenReturn(List.of(stock));

        PageResult<StockVO> result = stockService.getStockPage("P001", "商品A", 0, 0);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getPageNum());
        assertEquals(10, result.getPageSize());
        assertEquals(1, result.getList().size());
        verify(stockMapper).selectStockPage("P001", "商品A", 0, 10);
    }

    @Test
    void getStockPage_shouldClampPageSizeToMax() {
        when(stockMapper.countStock(null, null)).thenReturn(0L);
        when(stockMapper.selectStockPage(null, null, 0, 200)).thenReturn(List.of());

        PageResult<StockVO> result = stockService.getStockPage(null, null, 1, 999);

        assertEquals(200, result.getPageSize());
        verify(stockMapper).selectStockPage(null, null, 0, 200);
    }

    @Test
    void getStockPage_shouldCalculateOffsetForLaterPages() {
        StockVO stock = stock(2L, "P002", "商品B", 18, 6);
        when(stockMapper.countStock("P002", null)).thenReturn(1L);
        when(stockMapper.selectStockPage("P002", null, 20, 10)).thenReturn(List.of(stock));

        PageResult<StockVO> result = stockService.getStockPage("P002", null, 3, 10);

        assertEquals(3, result.getPageNum());
        assertEquals(10, result.getPageSize());
        assertEquals(1, result.getList().size());
        verify(stockMapper).selectStockPage("P002", null, 20, 10);
    }

    @Test
    void updateStock_shouldThrow_whenStockRecordMissing() {
        StockUpdateDTO dto = updateDto(10L, 20, 5, "盘点");
        when(stockMapper.selectByProductId(10L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> stockService.updateStock(dto));

        assertEquals("库存记录不存在", exception.getMessage());
        verify(stockMapper, never()).updateByProductId(any());
        verify(stockFlowService, never()).recordManualAdjust(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateStock_shouldThrow_whenQuantityMissing() {
        StockUpdateDTO dto = updateDto(10L, null, 5, "盘点");
        when(stockMapper.selectByProductId(10L)).thenReturn(stock(10L, "P010", "商品A", 8, 3));

        BusinessException exception = assertThrows(BusinessException.class, () -> stockService.updateStock(dto));

        assertEquals("库存数量不能为空", exception.getMessage());
        verify(stockMapper, never()).updateByProductId(any());
    }

    @Test
    void updateStock_shouldThrow_whenWarningQuantityMissing() {
        StockUpdateDTO dto = updateDto(10L, 20, null, "盘点");
        when(stockMapper.selectByProductId(10L)).thenReturn(stock(10L, "P010", "商品A", 8, 3));

        BusinessException exception = assertThrows(BusinessException.class, () -> stockService.updateStock(dto));

        assertEquals("预警库存不能为空", exception.getMessage());
        verify(stockMapper, never()).updateByProductId(any());
    }

    @Test
    void updateStock_shouldThrow_whenMapperUpdateFails() {
        StockUpdateDTO dto = updateDto(10L, 20, 5, "盘点");
        when(stockMapper.selectByProductId(10L)).thenReturn(stock(10L, "P010", "商品A", 8, 3));
        when(stockMapper.updateByProductId(dto)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> stockService.updateStock(dto));

        assertEquals("修改库存失败", exception.getMessage());
        verify(stockFlowService, never()).recordManualAdjust(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateStock_shouldUseDefaultOperatorAndReason_whenContextAndReasonEmpty() {
        StockUpdateDTO dto = updateDto(10L, 20, 5, "   ");
        when(stockMapper.selectByProductId(10L)).thenReturn(stock(10L, "P010", "商品A", null, 3));
        when(stockMapper.updateByProductId(dto)).thenReturn(1);

        String result = stockService.updateStock(dto);

        assertEquals("修改库存成功", result);
        verify(stockFlowService).recordManualAdjust(
                10L,
                "商品A",
                0,
                20,
                "admin",
                "手工修改库存",
                "库存管理页手工修改"
        );
    }

    @Test
    void updateStock_shouldTrimReasonAndUseCurrentUsername_whenPresent() {
        CurrentUserContext.setUsername(" tester ");

        StockUpdateDTO dto = updateDto(10L, 20, 5, "  盘点修正  ");
        when(stockMapper.selectByProductId(10L)).thenReturn(stock(10L, "P010", "商品A", 8, 3));
        when(stockMapper.updateByProductId(dto)).thenReturn(1);

        String result = stockService.updateStock(dto);

        assertEquals("修改库存成功", result);
        verify(stockFlowService).recordManualAdjust(
                10L,
                "商品A",
                8,
                20,
                "tester",
                "盘点修正",
                "库存管理页手工修改"
        );
    }

    @Test
    void exportStockExcel_shouldExportWorkbook_whenRecordsExist() throws Exception {
        StockVO stock = stock(10L, " P010 ", " 商品A ", 18, 5);
        stock.setSpecification(" 500g ");
        stock.setUnit(" 袋 ");
        stock.setCategory(" 食品 ");
        stock.setLowStock(1);
        when(stockMapper.selectStockList("P010", "商品A")).thenReturn(List.of(stock));

        byte[] bytes = stockService.exportStockExcel("P010", "商品A");

        assertTrue(bytes.length > 0);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals("库存列表", workbook.getSheetAt(0).getSheetName());
            assertEquals("商品编码", workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue());
            assertEquals("P010", workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
            assertEquals("商品A", workbook.getSheetAt(0).getRow(1).getCell(2).getStringCellValue());
            assertEquals("500g", workbook.getSheetAt(0).getRow(1).getCell(3).getStringCellValue());
            assertEquals("袋", workbook.getSheetAt(0).getRow(1).getCell(4).getStringCellValue());
            assertEquals("食品", workbook.getSheetAt(0).getRow(1).getCell(5).getStringCellValue());
            assertEquals(18, (int) workbook.getSheetAt(0).getRow(1).getCell(6).getNumericCellValue());
            assertEquals(5, (int) workbook.getSheetAt(0).getRow(1).getCell(7).getNumericCellValue());
            assertEquals("是", workbook.getSheetAt(0).getRow(1).getCell(8).getStringCellValue());
        }
    }

    @Test
    void exportStockCsv_shouldExportUtf8BomAndEscapedValues_whenRecordsExist() {
        StockVO stock = stock(10L, "P010", "商品\"A\"", 18, 5);
        stock.setSpecification("500g");
        stock.setUnit("袋");
        stock.setCategory("食品");
        stock.setLowStock(0);
        when(stockMapper.selectStockList(null, null)).thenReturn(List.of(stock));

        byte[] bytes = stockService.exportStockCsv(null, null);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("商品ID,商品编码,商品名称,规格,单位,分类,当前库存,预警值,低库存"));
        assertTrue(csv.contains("\"商品\"\"A\"\"\""));
        assertTrue(csv.contains("\"否\""));
    }

    private StockUpdateDTO updateDto(Long productId, Integer quantity, Integer warningQuantity, String reason) {
        StockUpdateDTO dto = new StockUpdateDTO();
        dto.setProductId(productId);
        dto.setQuantity(quantity);
        dto.setWarningQuantity(warningQuantity);
        dto.setReason(reason);
        return dto;
    }

    private StockVO stock(Long productId, String productCode, String productName, Integer quantity, Integer warningQuantity) {
        StockVO stock = new StockVO();
        stock.setProductId(productId);
        stock.setProductCode(productCode);
        stock.setProductName(productName);
        stock.setQuantity(quantity);
        stock.setWarningQuantity(warningQuantity);
        return stock;
    }
}
