package com.yocaihua.wms.controller;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.Result;
import com.yocaihua.wms.dto.InboundOrderAddDTO;
import com.yocaihua.wms.service.InboundOrderService;
import com.yocaihua.wms.vo.InboundOrderDetailVO;
import com.yocaihua.wms.vo.InboundOrderVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class InboundOrderController {

    private final InboundOrderService inboundOrderService;

    public InboundOrderController(InboundOrderService inboundOrderService) {
        this.inboundOrderService = inboundOrderService;
    }

    @PostMapping("/inbound-order/add")
    public Result<String> saveInboundOrder(@Valid @RequestBody InboundOrderAddDTO inboundOrderAddDTO) {
        return Result.success(inboundOrderService.saveInboundOrder(inboundOrderAddDTO));
    }

    @GetMapping("/inbound-order/list")
    public Result<PageResult<InboundOrderVO>> getInboundOrderPage(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        return Result.success(inboundOrderService.getInboundOrderPage(orderNo, pageNum, pageSize));
    }

    @GetMapping("/inbound-order/{id}")
    public Result<InboundOrderDetailVO> getInboundOrderDetail(@PathVariable Long id) {
        return Result.success(inboundOrderService.getInboundOrderDetail(id));
    }
}