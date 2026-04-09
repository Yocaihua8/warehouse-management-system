package com.yocaihua.wms.controller;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.Result;
import com.yocaihua.wms.dto.InboundOrderAddDTO;
import com.yocaihua.wms.service.InboundOrderService;
import com.yocaihua.wms.vo.InboundDetailVO;
import com.yocaihua.wms.vo.InboundOrderDetailVO;
import com.yocaihua.wms.vo.InboundOrderVO;
import com.yocaihua.wms.vo.OrderCreatedVO;
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

@Tag(name = "入库管理", description = "入库单的创建（草稿）、编辑、确认入库、作废，以及 Excel/PDF 导出")
@SecurityRequirement(name = "token")
@RestController
@RequestMapping("/inbound-order")
public class InboundOrderController {

    private final InboundOrderService inboundOrderService;

    public InboundOrderController(InboundOrderService inboundOrderService) {
        this.inboundOrderService = inboundOrderService;
    }

    @Operation(summary = "新建入库单（草稿）", description = "创建后状态为草稿（status=1），需管理员确认后才会更新库存")
    @PostMapping("/add")
    public Result<OrderCreatedVO> saveInboundOrder(@Valid @RequestBody InboundOrderAddDTO inboundOrderAddDTO) {
        return Result.success(inboundOrderService.saveInboundOrder(inboundOrderAddDTO));
    }

    @Operation(summary = "编辑入库单草稿", description = "仅草稿状态可编辑；会删除原有明细后重新插入")
    @PutMapping("/{id}")
    public Result<String> updateInboundOrderDraft(
            @Parameter(description = "入库单 ID") @PathVariable Long id,
            @Valid @RequestBody InboundOrderAddDTO inboundOrderAddDTO) {
        return Result.success(inboundOrderService.updateInboundOrderDraft(id, inboundOrderAddDTO));
    }

    @Operation(summary = "确认入库", description = "【Admin】将草稿单据转为已入库，同时增加对应商品库存并写入审计日志")
    @PostMapping("/{id}/confirm")
    public Result<String> confirmInboundOrder(@Parameter(description = "入库单 ID") @PathVariable Long id) {
        return Result.success(inboundOrderService.confirmInboundOrder(id));
    }

    @Operation(summary = "作废入库单", description = "【Admin】草稿直接作废；已入库单据作废时会回滚库存变更")
    @PostMapping("/{id}/void")
    public Result<String> voidInboundOrder(
            @Parameter(description = "入库单 ID") @PathVariable Long id,
            @Parameter(description = "作废原因") @RequestParam String voidReason) {
        return Result.success(inboundOrderService.voidInboundOrder(id, voidReason));
    }

    @Operation(summary = "入库单分页列表", description = "支持按单号、来源类型（MANUAL/AI）、状态筛选")
    @GetMapping("/list")
    public Result<PageResult<InboundOrderVO>> getInboundOrderPage(
            @Parameter(description = "单号（模糊）") @RequestParam(required = false) String orderNo,
            @Parameter(description = "来源：MANUAL（手动）/ AI（AI识别）") @RequestParam(required = false) String sourceType,
            @Parameter(description = "状态：1草稿 / 2已入库 / 3作废") @RequestParam(required = false) Integer orderStatus,
            @Parameter(description = "页码，默认 1") @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "每页条数，默认 10") @RequestParam(required = false) Integer pageSize) {
        return Result.success(inboundOrderService.getInboundOrderPage(orderNo, sourceType, orderStatus, pageNum, pageSize));
    }

    @Operation(summary = "入库单详情（含明细）")
    @GetMapping("/detail/{id}")
    public Result<InboundOrderDetailVO> getInboundOrderDetail(@Parameter(description = "入库单 ID") @PathVariable Long id) {
        return Result.success(inboundOrderService.getInboundOrderDetail(id));
    }

    @Operation(summary = "导出入库单 Excel")
    @GetMapping("/{id}/export/excel")
    public ResponseEntity<byte[]> exportInboundOrderExcel(@Parameter(description = "入库单 ID") @PathVariable Long id) {
        byte[] content = inboundOrderService.exportInboundOrderExcel(id);
        String fileName = "入库单-" + id + ".xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @Operation(summary = "导出入库单 PDF")
    @GetMapping("/{id}/export/pdf")
    public ResponseEntity<byte[]> exportInboundOrderPdf(@Parameter(description = "入库单 ID") @PathVariable Long id) {
        byte[] content = inboundOrderService.exportInboundOrderPdf(id);
        String fileName = "入库单-" + id + ".pdf";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    @Operation(summary = "入库单简要详情", description = "与 /detail/{id} 返回字段略有差异，用于打印/预览场景")
    @GetMapping("/{id}")
    public Result<InboundDetailVO> getDetail(@Parameter(description = "入库单 ID") @PathVariable Long id) {
        return Result.success(inboundOrderService.getDetail(id));
    }
}
