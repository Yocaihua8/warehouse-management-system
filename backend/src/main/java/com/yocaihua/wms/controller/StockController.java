package com.yocaihua.wms.controller;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.Result;
import com.yocaihua.wms.dto.StockUpdateDTO;
import com.yocaihua.wms.service.LowStockAlertService;
import com.yocaihua.wms.service.StockService;
import com.yocaihua.wms.vo.StockVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "库存管理", description = "库存查询、手动调整、导出（Excel/CSV）")
@SecurityRequirement(name = "token")
@RestController
public class StockController {

    private final StockService stockService;
    private final LowStockAlertService lowStockAlertService;

    public StockController(StockService stockService,
                           LowStockAlertService lowStockAlertService) {
        this.stockService = stockService;
        this.lowStockAlertService = lowStockAlertService;
    }

    @Operation(summary = "库存分页列表", description = "支持按商品编码、名称筛选；`lowStock=true` 可过滤预警商品（推测，视 Service 实现）")
    @GetMapping("/stock/list")
    public Result<PageResult<StockVO>> getStockPage(
            @Parameter(description = "商品编码（模糊）") @RequestParam(required = false) String productCode,
            @Parameter(description = "商品名称（模糊）") @RequestParam(required = false) String productName,
            @Parameter(description = "页码，默认 1") @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "每页条数，默认 10") @RequestParam(required = false) Integer pageSize) {
        return Result.success(stockService.getStockPage(productCode, productName, pageNum, pageSize));
    }

    @Operation(summary = "导出库存文件", description = "format=excel（默认）返回 .xlsx；format=csv 返回 .csv")
    @GetMapping("/stock/export")
    public ResponseEntity<byte[]> exportStock(
            @Parameter(description = "商品编码（模糊）") @RequestParam(required = false) String productCode,
            @Parameter(description = "商品名称（模糊）") @RequestParam(required = false) String productName,
            @Parameter(description = "导出格式：excel / csv，默认 excel") @RequestParam(required = false, defaultValue = "excel") String format) {
        String normalizedFormat = format == null ? "excel" : format.trim().toLowerCase();
        if ("csv".equals(normalizedFormat)) {
            byte[] content = stockService.exportStockCsv(productCode, productName);
            String fileName = "库存列表.csv";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                    .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                    .body(content);
        }

        byte[] content = stockService.exportStockExcel(productCode, productName);
        String fileName = "库存列表.xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @Operation(summary = "手动调整库存", description = "【Admin】直接修改指定商品的库存数量，并写入 stock_adjust_log 审计记录")
    @PutMapping("/stock/update")
    public Result<String> updateStock(@RequestBody StockUpdateDTO stockUpdateDTO) {
        return Result.success(stockService.updateStock(stockUpdateDTO));
    }

    @Operation(summary = "手动触发低库存预警通知", description = "【Admin】立即执行一次低库存扫描与通知发送（邮件/Webhook）")
    @PostMapping("/stock/low-alert/trigger")
    public Result<String> triggerLowStockAlert() {
        return Result.success(lowStockAlertService.triggerNow());
    }
}
