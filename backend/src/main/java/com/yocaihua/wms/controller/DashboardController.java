package com.yocaihua.wms.controller;

import com.yocaihua.wms.common.Result;
import com.yocaihua.wms.service.DashboardService;
import com.yocaihua.wms.vo.DashboardTrendPointVO;
import com.yocaihua.wms.vo.DashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "数据看板", description = "首页统计数据：商品数、客户数、库存总量、低库存预警数、入库单数、出库单数")
@SecurityRequirement(name = "token")
@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "获取首页统计数据")
    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboardData() {
        return Result.success(dashboardService.getDashboardData());
    }

    @Operation(summary = "获取近N天入库/出库趋势")
    @GetMapping("/dashboard/trend")
    public Result<List<DashboardTrendPointVO>> getDashboardTrend(
            @RequestParam(required = false, defaultValue = "7") Integer days) {
        int normalizedDays = days == null ? 7 : days;
        return Result.success(dashboardService.getRecentTrend(normalizedDays));
    }
}
