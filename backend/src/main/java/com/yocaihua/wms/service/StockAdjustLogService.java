package com.yocaihua.wms.service;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.vo.StockAdjustLogVO;

public interface StockAdjustLogService {

    PageResult<StockAdjustLogVO> getLogPage(String productName, Integer pageNum, Integer pageSize);
}