package com.yocaihua.wms.controller;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.Result;
import com.yocaihua.wms.dto.OutboundOrderAddDTO;
import com.yocaihua.wms.service.OutboundOrderService;
import com.yocaihua.wms.vo.OutboundOrderDetailVO;
import com.yocaihua.wms.vo.OutboundOrderVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class OutboundOrderController {

    private final OutboundOrderService outboundOrderService;

    public OutboundOrderController(OutboundOrderService outboundOrderService) {
        this.outboundOrderService = outboundOrderService;
    }

    @PostMapping("/outbound-order/add")
    public Result<String> saveOutboundOrder(@Valid @RequestBody OutboundOrderAddDTO outboundOrderAddDTO) {
        return Result.success(outboundOrderService.saveOutboundOrder(outboundOrderAddDTO));
    }

    @GetMapping("/outbound-order/list")
    public Result<PageResult<OutboundOrderVO>> getOutboundOrderPage(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        return Result.success(outboundOrderService.getOutboundOrderPage(orderNo, pageNum, pageSize));
    }

    @GetMapping("/outbound-order/{id}")
    public Result<OutboundOrderDetailVO> getOutboundOrderDetail(@PathVariable Long id) {
        return Result.success(outboundOrderService.getOutboundOrderDetail(id));
    }

}