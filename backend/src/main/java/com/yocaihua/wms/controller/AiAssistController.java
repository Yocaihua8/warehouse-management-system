package com.yocaihua.wms.controller;

import com.yocaihua.wms.common.OperatorHolder;
import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.service.AiRecognitionService;
import com.yocaihua.wms.common.Result;
import com.yocaihua.wms.dto.AiInboundConfirmDTO;
import com.yocaihua.wms.dto.AiOutboundConfirmDTO;
import com.yocaihua.wms.vo.AiInboundRecognizeVO;
import com.yocaihua.wms.vo.AiOutboundRecognizeVO;
import com.yocaihua.wms.vo.AiRecognitionRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@Tag(name = "AI 辅助识别", description = "上传单据图片 → AI OCR 识别 → 人工复核 → 确认生成正式入库/出库单")
@SecurityRequirement(name = "token")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiAssistController {

    private final AiRecognitionService aiRecognitionService;

    @Operation(summary = "AI 服务连通性测试")
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.success("ai controller ok");
    }

    @Operation(
        summary = "识别入库单图片",
        description = "上传图片，调用 Python OCR 服务识别商品明细，自动尝试匹配系统中的商品和供应商，返回待确认结果"
    )
    @PostMapping("/inbound/recognize")
    public Result<AiInboundRecognizeVO> recognizeInbound(
            @Parameter(description = "单据图片文件") @RequestParam(value = "file", required = false) MultipartFile file) {
        AiInboundRecognizeVO result = aiRecognitionService.recognizeInbound(file, OperatorHolder.getCurrentOperator());
        return Result.success(result);
    }

    @Operation(
        summary = "确认 AI 入库识别结果",
        description = "【Admin】用户核对/修正识别结果后提交，系统创建正式入库单并直接完成入库（更新库存）"
    )
    @PostMapping("/inbound/confirm")
    public Result<Long> confirmInbound(@RequestBody AiInboundConfirmDTO dto) {
        Long orderId = aiRecognitionService.confirmInbound(dto, OperatorHolder.getCurrentOperator());
        return Result.success(orderId);
    }

    @Operation(
        summary = "识别出库单图片",
        description = "上传图片，OCR 识别商品明细，自动尝试匹配系统商品和客户"
    )
    @PostMapping("/outbound/recognize")
    public Result<AiOutboundRecognizeVO> recognizeOutbound(
            @Parameter(description = "单据图片文件") @RequestParam(value = "file", required = false) MultipartFile file) {
        AiOutboundRecognizeVO result = aiRecognitionService.recognizeOutbound(file, OperatorHolder.getCurrentOperator());
        return Result.success(result);
    }

    @Operation(
        summary = "确认 AI 出库识别结果",
        description = "【Admin】用户核对/修正识别结果后提交，系统创建正式出库单并直接完成出库（扣减库存）"
    )
    @PostMapping("/outbound/confirm")
    public Result<Long> confirmOutbound(@RequestBody AiOutboundConfirmDTO dto) {
        Long orderId = aiRecognitionService.confirmOutbound(dto, OperatorHolder.getCurrentOperator());
        return Result.success(orderId);
    }

    @Operation(summary = "AI 入库识别历史记录列表", description = "返回所有入库识别任务，包括待确认、已确认、识别失败等状态")
    @GetMapping("/inbound/list")
    public Result<PageResult<AiRecognitionRecordVO>> listInboundRecords(
            @Parameter(description = "页码，默认 1") @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "每页条数，默认 10") @RequestParam(required = false) Integer pageSize) {
        return Result.success(aiRecognitionService.listInboundRecords(pageNum, pageSize));
    }

    @Operation(summary = "AI 入库识别记录详情")
    @GetMapping("/inbound/detail/{id}")
    public Result<AiInboundRecognizeVO> getInboundRecordDetail(
            @Parameter(description = "识别记录 ID") @PathVariable Long id) {
        AiInboundRecognizeVO detail = aiRecognitionService.getInboundRecordDetail(id);
        return Result.success(detail);
    }

    @Operation(summary = "AI 出库识别历史记录列表", description = "返回所有出库识别任务，包括待确认、已确认、识别失败等状态")
    @GetMapping("/outbound/list")
    public Result<PageResult<AiRecognitionRecordVO>> listOutboundRecords(
            @Parameter(description = "页码，默认 1") @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "每页条数，默认 10") @RequestParam(required = false) Integer pageSize) {
        return Result.success(aiRecognitionService.listOutboundRecords(pageNum, pageSize));
    }

    @Operation(summary = "AI 出库识别记录详情")
    @GetMapping("/outbound/detail/{id}")
    public Result<AiOutboundRecognizeVO> getOutboundRecordDetail(
            @Parameter(description = "识别记录 ID") @PathVariable Long id) {
        AiOutboundRecognizeVO detail = aiRecognitionService.getOutboundRecordDetail(id);
        return Result.success(detail);
    }
}
