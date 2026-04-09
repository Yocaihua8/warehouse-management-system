package com.yocaihua.wms.controller;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.Result;
import com.yocaihua.wms.service.StockAdjustLogService;
import com.yocaihua.wms.vo.StockAdjustLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "库存流水", description = "所有库存变更的审计日志，包括手动调整、入库、出库、AI 确认等")
@SecurityRequirement(name = "token")
@RestController
public class StockAdjustLogController {

    private final StockAdjustLogService stockAdjustLogService;

    public StockAdjustLogController(StockAdjustLogService stockAdjustLogService) {
        this.stockAdjustLogService = stockAdjustLogService;
    }

    @Operation(summary = "库存流水分页列表", description = "按操作时间倒序排列；changeType 包含：MANUAL_ADJUST / MANUAL_INBOUND / MANUAL_OUTBOUND / AI_CONFIRM_INBOUND / AI_CONFIRM_OUTBOUND")
    @GetMapping("/stock/log/list")
    public Result<PageResult<StockAdjustLogVO>> getLogPage(
            @Parameter(description = "商品名称（模糊）") @RequestParam(required = false) String productName,
            @Parameter(description = "页码，默认 1") @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "每页条数，默认 10") @RequestParam(required = false) Integer pageSize) {
        return Result.success(stockAdjustLogService.getLogPage(productName, pageNum, pageSize));
    }
}
