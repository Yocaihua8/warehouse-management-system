package com.yocaihua.wms.service;

import com.yocaihua.wms.dto.StockUpdateDTO;
import com.yocaihua.wms.vo.StockVO;

import java.util.List;

public interface StockService {

    List<StockVO> getStockList(String productName);

    String updateStock(StockUpdateDTO stockUpdateDTO);


}