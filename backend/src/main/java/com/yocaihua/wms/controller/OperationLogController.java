package com.yocaihua.wms.controller;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.Result;
import com.yocaihua.wms.service.OperationLogService;
import com.yocaihua.wms.vo.OperationLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "操作日志", description = "记录用户关键操作，如登录、确认入库/出库、作废单据")
@SecurityRequirement(name = "token")
@RestController
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Operation(summary = "操作日志分页列表", description = "按操作时间倒序")
    @GetMapping("/operation/log/list")
    public Result<PageResult<OperationLogVO>> getLogPage(
            @Parameter(description = "操作类型（精确）") @RequestParam(required = false) String actionType,
            @Parameter(description = "模块名称（模糊）") @RequestParam(required = false) String moduleName,
            @Parameter(description = "操作人（模糊）") @RequestParam(required = false) String operatorName,
            @Parameter(description = "结果状态（SUCCESS/FAILED）") @RequestParam(required = false) String resultStatus,
            @Parameter(description = "关联单号（模糊）") @RequestParam(required = false) String bizNo,
            @Parameter(description = "页码，默认 1") @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "每页条数，默认 10") @RequestParam(required = false) Integer pageSize) {
        return Result.success(operationLogService.getLogPage(
                actionType,
                moduleName,
                operatorName,
                resultStatus,
                bizNo,
                pageNum,
                pageSize
        ));
    }
}

