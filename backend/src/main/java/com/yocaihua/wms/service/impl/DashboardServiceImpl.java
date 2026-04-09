package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.mapper.DashboardMapper;
import com.yocaihua.wms.service.DashboardService;
import com.yocaihua.wms.vo.DashboardDailyCountRowVO;
import com.yocaihua.wms.vo.DashboardTrendPointVO;
import com.yocaihua.wms.vo.DashboardVO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final DashboardMapper dashboardMapper;

    public DashboardServiceImpl(DashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    @Override
    @Cacheable(cacheNames = "dashboardSummary")
    public DashboardVO getDashboardData() {
        DashboardVO dashboardVO = new DashboardVO();
        dashboardVO.setProductCount(dashboardMapper.countProduct());
        dashboardVO.setCustomerCount(dashboardMapper.countCustomer());
        dashboardVO.setStockProductCount(dashboardMapper.countStockProduct());
        dashboardVO.setLowStockCount(dashboardMapper.countLowStock());
        dashboardVO.setInboundOrderCount(dashboardMapper.countInboundOrder());
        dashboardVO.setOutboundOrderCount(dashboardMapper.countOutboundOrder());
        return dashboardVO;
    }

    @Override
    @Cacheable(cacheNames = "dashboardTrend", key = "#days")
    public List<DashboardTrendPointVO> getRecentTrend(int days) {
        if (days < 1 || days > 60) {
            throw new BusinessException("趋势天数参数无效，范围 1-60");
        }

        LocalDate startDate = LocalDate.now().minusDays(days - 1L);
        Map<String, Long> inboundCountMap = toDailyCountMap(dashboardMapper.selectInboundDailyCount(startDate));
        Map<String, Long> outboundCountMap = toDailyCountMap(dashboardMapper.selectOutboundDailyCount(startDate));

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        return startDate.datesUntil(LocalDate.now().plusDays(1))
                .map(date -> {
                    String dateKey = formatter.format(date);
                    DashboardTrendPointVO point = new DashboardTrendPointVO();
                    point.setStatDate(dateKey);
                    point.setInboundCount(inboundCountMap.getOrDefault(dateKey, 0L));
                    point.setOutboundCount(outboundCountMap.getOrDefault(dateKey, 0L));
                    return point;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Long> toDailyCountMap(List<DashboardDailyCountRowVO> rows) {
        Map<String, Long> countMap = new HashMap<>();
        if (rows == null || rows.isEmpty()) {
            return countMap;
        }
        for (DashboardDailyCountRowVO row : rows) {
            if (row == null || row.getStatDate() == null) {
                continue;
            }
            countMap.put(row.getStatDate(), row.getTotal() == null ? 0L : row.getTotal());
        }
        return countMap;
    }
}
