package com.yocaihua.wms.controller;

import com.yocaihua.wms.common.Result;
import com.yocaihua.wms.dto.StockUpdateDTO;
import com.yocaihua.wms.service.StockService;
import com.yocaihua.wms.vo.StockVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/stock/list")
    public Result<List<StockVO>> getStockList(
            @RequestParam(required = false) String productName) {
        return Result.success(stockService.getStockList(productName));
    }

    @PutMapping("/stock/update")
    public Result<String> updateStock(@RequestBody StockUpdateDTO stockUpdateDTO) {
        return Result.success(stockService.updateStock(stockUpdateDTO));
    }
}