package com.yocaihua.wms.service;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.dto.StockUpdateDTO;
import com.yocaihua.wms.vo.StockVO;

public interface StockService {

    PageResult<StockVO> getStockPage(String productCode, String productName, Integer pageNum, Integer pageSize);

    String updateStock(StockUpdateDTO stockUpdateDTO);

    byte[] exportStockExcel(String productCode, String productName);

    byte[] exportStockCsv(String productCode, String productName);

}
