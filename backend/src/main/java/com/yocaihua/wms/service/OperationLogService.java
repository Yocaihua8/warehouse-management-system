package com.yocaihua.wms.service;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.vo.OperationLogVO;

public interface OperationLogService {

    void recordSuccess(String actionType,
                       String moduleName,
                       String bizType,
                       Long bizId,
                       String bizNo,
                       String operatorName,
                       String message);

    PageResult<OperationLogVO> getLogPage(String actionType,
                                          String moduleName,
                                          String operatorName,
                                          String resultStatus,
                                          String bizNo,
                                          Integer pageNum,
                                          Integer pageSize);
}

