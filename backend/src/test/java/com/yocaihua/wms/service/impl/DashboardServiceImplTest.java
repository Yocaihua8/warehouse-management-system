package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.mapper.DashboardMapper;
import com.yocaihua.wms.vo.DashboardDailyCountRowVO;
import com.yocaihua.wms.vo.DashboardTrendPointVO;
import com.yocaihua.wms.vo.DashboardVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private DashboardMapper dashboardMapper;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Test
    void getDashboardData_shouldAssembleAllCounters() {
        when(dashboardMapper.countProduct()).thenReturn(10L);
        when(dashboardMapper.countCustomer()).thenReturn(8L);
        when(dashboardMapper.countStockProduct()).thenReturn(6L);
        when(dashboardMapper.countLowStock()).thenReturn(2L);
        when(dashboardMapper.countInboundOrder()).thenReturn(12L);
        when(dashboardMapper.countOutboundOrder()).thenReturn(7L);

        DashboardVO result = dashboardService.getDashboardData();

        assertEquals(10L, result.getProductCount());
        assertEquals(8L, result.getCustomerCount());
        assertEquals(6L, result.getStockProductCount());
        assertEquals(2L, result.getLowStockCount());
        assertEquals(12L, result.getInboundOrderCount());
        assertEquals(7L, result.getOutboundOrderCount());
    }

    @Test
    void getRecentTrend_shouldThrow_whenDaysBelowRange() {
        BusinessException exception = assertThrows(BusinessException.class, () -> dashboardService.getRecentTrend(0));

        assertEquals("趋势天数参数无效，范围 1-60", exception.getMessage());
    }

    @Test
    void getRecentTrend_shouldThrow_whenDaysAboveRange() {
        BusinessException exception = assertThrows(BusinessException.class, () -> dashboardService.getRecentTrend(61));

        assertEquals("趋势天数参数无效，范围 1-60", exception.getMessage());
    }

    @Test
    void getRecentTrend_shouldReturnSingleTodayPoint_whenDaysIsOne() {
        LocalDate today = LocalDate.now();
        String todayText = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
        when(dashboardMapper.selectInboundDailyCount(today)).thenReturn(List.of(row(todayText, 3L)));
        when(dashboardMapper.selectOutboundDailyCount(today)).thenReturn(List.of(row(todayText, 1L)));

        List<DashboardTrendPointVO> result = dashboardService.getRecentTrend(1);

        assertEquals(1, result.size());
        assertEquals(todayText, result.get(0).getStatDate());
        assertEquals(3L, result.get(0).getInboundCount());
        assertEquals(1L, result.get(0).getOutboundCount());
        verify(dashboardMapper).selectInboundDailyCount(today);
        verify(dashboardMapper).selectOutboundDailyCount(today);
    }

    @Test
    void getRecentTrend_shouldFillMissingDatesAndNormalizeNullTotals() {
        LocalDate startDate = LocalDate.now().minusDays(2);
        String day1 = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String day2 = startDate.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String day3 = startDate.plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE);

        when(dashboardMapper.selectInboundDailyCount(startDate)).thenReturn(List.of(
                row(day1, 5L),
                row(day3, null)
        ));
        when(dashboardMapper.selectOutboundDailyCount(startDate)).thenReturn(List.of(
                row(day2, 2L)
        ));

        List<DashboardTrendPointVO> result = dashboardService.getRecentTrend(3);

        assertEquals(3, result.size());
        assertEquals(day1, result.get(0).getStatDate());
        assertEquals(5L, result.get(0).getInboundCount());
        assertEquals(0L, result.get(0).getOutboundCount());

        assertEquals(day2, result.get(1).getStatDate());
        assertEquals(0L, result.get(1).getInboundCount());
        assertEquals(2L, result.get(1).getOutboundCount());

        assertEquals(day3, result.get(2).getStatDate());
        assertEquals(0L, result.get(2).getInboundCount());
        assertEquals(0L, result.get(2).getOutboundCount());
    }

    @Test
    void getRecentTrend_shouldIgnoreNullRowsAndNullDates() {
        LocalDate startDate = LocalDate.now().minusDays(1);
        String day1 = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String day2 = startDate.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);

        when(dashboardMapper.selectInboundDailyCount(startDate)).thenReturn(Arrays.asList(
                null,
                row(null, 9L),
                row(day1, 4L)
        ));
        when(dashboardMapper.selectOutboundDailyCount(startDate)).thenReturn(null);

        List<DashboardTrendPointVO> result = dashboardService.getRecentTrend(2);

        assertEquals(2, result.size());
        assertEquals(day1, result.get(0).getStatDate());
        assertEquals(4L, result.get(0).getInboundCount());
        assertEquals(0L, result.get(0).getOutboundCount());
        assertEquals(day2, result.get(1).getStatDate());
        assertEquals(0L, result.get(1).getInboundCount());
        assertEquals(0L, result.get(1).getOutboundCount());
    }

    private DashboardDailyCountRowVO row(String statDate, Long total) {
        DashboardDailyCountRowVO row = new DashboardDailyCountRowVO();
        row.setStatDate(statDate);
        row.setTotal(total);
        return row;
    }
}
