package com.yocaihua.wms.controller;

import com.yocaihua.wms.common.Result;
import com.yocaihua.wms.service.SystemService;
import com.yocaihua.wms.vo.SystemBootstrapVO;
import com.yocaihua.wms.vo.SystemHealthVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "系统", description = "健康检查、启动信息（无需鉴权）")
@RestController
public class SystemController {

    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    @Operation(summary = "系统健康检查", description = "返回数据库连接、AI 服务等运行状态，无需登录即可访问")
    @GetMapping("/system/health")
    public Result<SystemHealthVO> health() {
        return Result.success(systemService.getSystemHealth());
    }

    @Operation(summary = "系统启动信息", description = "返回系统版本号、名称等初始化信息，无需登录即可访问")
    @GetMapping("/system/bootstrap")
    public Result<SystemBootstrapVO> bootstrap() {
        return Result.success(systemService.getSystemBootstrap());
    }
}
