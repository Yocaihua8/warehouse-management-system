package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.mapper.StockAdjustLogMapper;
import com.yocaihua.wms.service.StockAdjustLogService;
import com.yocaihua.wms.vo.StockAdjustLogVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockAdjustLogServiceImpl implements StockAdjustLogService {

    private static final int MAX_PAGE_SIZE = 200;

    private final StockAdjustLogMapper stockAdjustLogMapper;

    public StockAdjustLogServiceImpl(StockAdjustLogMapper stockAdjustLogMapper) {
        this.stockAdjustLogMapper = stockAdjustLogMapper;
    }

    @Override
    public PageResult<StockAdjustLogVO> getLogPage(String productName, Integer pageNum, Integer pageSize) {
        int currentPage = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int currentSize = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        if (currentSize > MAX_PAGE_SIZE) {
            currentSize = MAX_PAGE_SIZE;
        }
        int offset = (currentPage - 1) * currentSize;

        Long total = stockAdjustLogMapper.countLog(productName);
        List<StockAdjustLogVO> records = stockAdjustLogMapper.selectLogPage(productName, offset, currentSize);

        return new PageResult<>(total, currentPage, currentSize, records);
    }
}
