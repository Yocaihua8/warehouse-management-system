package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.dto.StockUpdateDTO;
import com.yocaihua.wms.mapper.StockMapper;
import com.yocaihua.wms.service.StockService;
import com.yocaihua.wms.vo.StockVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockServiceImpl implements StockService {

    private final StockMapper stockMapper;

    public StockServiceImpl(StockMapper stockMapper) {
        this.stockMapper = stockMapper;
    }

    @Override
    public List<StockVO> getStockList(String productName) {
        return stockMapper.selectStockList(productName);
    }

    @Override
    public String updateStock(StockUpdateDTO stockUpdateDTO) {
        StockVO stockVO = stockMapper.selectByProductId(stockUpdateDTO.getProductId());
        if (stockVO == null) {
            throw new BusinessException("库存记录不存在");
        }

        int rows = stockMapper.updateByProductId(stockUpdateDTO);
        if (rows > 0) {
            return "修改库存成功";
        }
        throw new BusinessException("修改库存失败");
    }
}