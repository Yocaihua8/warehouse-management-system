package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.mapper.StockAdjustLogMapper;
import com.yocaihua.wms.vo.StockAdjustLogVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockAdjustLogServiceImplTest {

    @Mock
    private StockAdjustLogMapper stockAdjustLogMapper;

    @InjectMocks
    private StockAdjustLogServiceImpl stockAdjustLogService;

    @Test
    void getLogPage_shouldUseDefaultPagination_whenPageParamsAreNull() {
        StockAdjustLogVO vo = logVo(1L, "商品A");
        when(stockAdjustLogMapper.countLog("商品A")).thenReturn(1L);
        when(stockAdjustLogMapper.selectLogPage("商品A", 0, 10)).thenReturn(List.of(vo));

        PageResult<StockAdjustLogVO> result = stockAdjustLogService.getLogPage("商品A", null, null);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getPageNum());
        assertEquals(10, result.getPageSize());
        assertEquals(1, result.getList().size());
        verify(stockAdjustLogMapper).countLog("商品A");
        verify(stockAdjustLogMapper).selectLogPage("商品A", 0, 10);
    }

    @Test
    void getLogPage_shouldUseDefaultPagination_whenPageParamsBelowOne() {
        when(stockAdjustLogMapper.countLog(null)).thenReturn(0L);
        when(stockAdjustLogMapper.selectLogPage(null, 0, 10)).thenReturn(List.of());

        PageResult<StockAdjustLogVO> result = stockAdjustLogService.getLogPage(null, 0, -1);

        assertEquals(0L, result.getTotal());
        assertEquals(1, result.getPageNum());
        assertEquals(10, result.getPageSize());
        assertEquals(0, result.getList().size());
        verify(stockAdjustLogMapper).countLog(null);
        verify(stockAdjustLogMapper).selectLogPage(null, 0, 10);
    }

    @Test
    void getLogPage_shouldClampPageSizeToMaxLimit() {
        when(stockAdjustLogMapper.countLog("商品B")).thenReturn(3L);
        when(stockAdjustLogMapper.selectLogPage("商品B", 0, 200)).thenReturn(List.of(
                logVo(1L, "商品B"),
                logVo(2L, "商品B")
        ));

        PageResult<StockAdjustLogVO> result = stockAdjustLogService.getLogPage("商品B", 1, 999);

        assertEquals(3L, result.getTotal());
        assertEquals(1, result.getPageNum());
        assertEquals(200, result.getPageSize());
        assertEquals(2, result.getList().size());
        verify(stockAdjustLogMapper).countLog("商品B");
        verify(stockAdjustLogMapper).selectLogPage("商品B", 0, 200);
    }

    @Test
    void getLogPage_shouldCalculateOffsetFromCurrentPage() {
        when(stockAdjustLogMapper.countLog("商品C")).thenReturn(5L);
        when(stockAdjustLogMapper.selectLogPage("商品C", 20, 10)).thenReturn(List.of(logVo(3L, "商品C")));

        PageResult<StockAdjustLogVO> result = stockAdjustLogService.getLogPage("商品C", 3, 10);

        assertEquals(5L, result.getTotal());
        assertEquals(3, result.getPageNum());
        assertEquals(10, result.getPageSize());
        assertEquals(1, result.getList().size());
        verify(stockAdjustLogMapper).countLog("商品C");
        verify(stockAdjustLogMapper).selectLogPage("商品C", 20, 10);
    }

    @Test
    void getLogPage_shouldPassProductNameAsIs_withoutTrimNormalization() {
        String rawKeyword = "  商品关键字  ";
        when(stockAdjustLogMapper.countLog(rawKeyword)).thenReturn(0L);
        when(stockAdjustLogMapper.selectLogPage(rawKeyword, 0, 10)).thenReturn(List.of());

        PageResult<StockAdjustLogVO> result = stockAdjustLogService.getLogPage(rawKeyword, 1, 10);

        assertEquals(0L, result.getTotal());
        assertEquals(1, result.getPageNum());
        assertEquals(10, result.getPageSize());
        verify(stockAdjustLogMapper).countLog(rawKeyword);
        verify(stockAdjustLogMapper).selectLogPage(rawKeyword, 0, 10);
    }

    private StockAdjustLogVO logVo(Long id, String productName) {
        StockAdjustLogVO vo = new StockAdjustLogVO();
        vo.setId(id);
        vo.setProductName(productName);
        return vo;
    }
}
