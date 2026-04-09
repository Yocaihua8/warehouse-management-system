package com.yocaihua.wms.controller;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.Result;
import com.yocaihua.wms.dto.OutboundOrderAddDTO;
import com.yocaihua.wms.service.OutboundOrderService;
import com.yocaihua.wms.vo.OrderCreatedVO;
import com.yocaihua.wms.vo.OutboundOrderDetailVO;
import com.yocaihua.wms.vo.OutboundOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "出库管理", description = "出库单的创建（草稿）、编辑、确认出库、作废，以及 Excel/PDF 导出。创建时即校验库存是否充足")
@SecurityRequirement(name = "token")
@RestController
public class OutboundOrderController {

    private final OutboundOrderService outboundOrderService;

    public OutboundOrderController(OutboundOrderService outboundOrderService) {
        this.outboundOrderService = outboundOrderService;
    }

    @Operation(summary = "新建出库单（草稿）", description = "创建时校验所有商品库存是否充足，不足则拒绝；创建后状态为草稿（status=1）")
    @PostMapping("/outbound-order/add")
    public Result<OrderCreatedVO> saveOutboundOrder(@Valid @RequestBody OutboundOrderAddDTO outboundOrderAddDTO) {
        return Result.success(outboundOrderService.saveOutboundOrder(outboundOrderAddDTO));
    }

    @Operation(summary = "编辑出库单草稿", description = "仅草稿状态可编辑")
    @PutMapping("/outbound-order/{id}")
    public Result<String> updateOutboundOrderDraft(
            @Parameter(description = "出库单 ID") @PathVariable Long id,
            @Valid @RequestBody OutboundOrderAddDTO outboundOrderAddDTO) {
        return Result.success(outboundOrderService.updateOutboundOrderDraft(id, outboundOrderAddDTO));
    }

    @Operation(summary = "确认出库", description = "【Admin】将草稿转为已出库，同时扣减对应商品库存并写入审计日志")
    @PostMapping("/outbound-order/{id}/confirm")
    public Result<String> confirmOutboundOrder(@Parameter(description = "出库单 ID") @PathVariable Long id) {
        return Result.success(outboundOrderService.confirmOutboundOrder(id));
    }

    @Operation(summary = "作废出库单", description = "【Admin】草稿直接作废；已出库单据作废时会回滚库存变更")
    @PostMapping("/outbound-order/{id}/void")
    public Result<String> voidOutboundOrder(
            @Parameter(description = "出库单 ID") @PathVariable Long id,
            @Parameter(description = "作废原因") @RequestParam String voidReason) {
        return Result.success(outboundOrderService.voidOutboundOrder(id, voidReason));
    }

    @Operation(summary = "出库单分页列表")
    @GetMapping("/outbound-order/list")
    public Result<PageResult<OutboundOrderVO>> getOutboundOrderPage(
            @Parameter(description = "单号（模糊）") @RequestParam(required = false) String orderNo,
            @Parameter(description = "状态：1草稿 / 2已出库 / 3作废") @RequestParam(required = false) Integer orderStatus,
            @Parameter(description = "页码，默认 1") @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "每页条数，默认 10") @RequestParam(required = false) Integer pageSize) {
        return Result.success(outboundOrderService.getOutboundOrderPage(orderNo, orderStatus, pageNum, pageSize));
    }

    @Operation(summary = "出库单详情（含明细）")
    @GetMapping("/outbound-order/{id}")
    public Result<OutboundOrderDetailVO> getOutboundOrderDetail(@Parameter(description = "出库单 ID") @PathVariable Long id) {
        return Result.success(outboundOrderService.getOutboundOrderDetail(id));
    }

    @Operation(summary = "导出出库单 Excel")
    @GetMapping("/outbound-order/{id}/export/excel")
    public ResponseEntity<byte[]> exportOutboundOrderExcel(@Parameter(description = "出库单 ID") @PathVariable Long id) {
        byte[] content = outboundOrderService.exportOutboundOrderExcel(id);
        String fileName = "出库单-" + id + ".xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @Operation(summary = "导出出库单 PDF")
    @GetMapping("/outbound-order/{id}/export/pdf")
    public ResponseEntity<byte[]> exportOutboundOrderPdf(@Parameter(description = "出库单 ID") @PathVariable Long id) {
        byte[] content = outboundOrderService.exportOutboundOrderPdf(id);
        String fileName = "出库单-" + id + ".pdf";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }
}
