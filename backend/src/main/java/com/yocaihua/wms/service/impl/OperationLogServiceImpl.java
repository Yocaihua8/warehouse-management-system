package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.entity.OperationLog;
import com.yocaihua.wms.mapper.OperationLogMapper;
import com.yocaihua.wms.service.OperationLogService;
import com.yocaihua.wms.vo.OperationLogVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogServiceImpl.class);
    private static final int MAX_PAGE_SIZE = 200;

    private final OperationLogMapper operationLogMapper;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    public void recordSuccess(String actionType,
                              String moduleName,
                              String bizType,
                              Long bizId,
                              String bizNo,
                              String operatorName,
                              String message) {
        try {
            OperationLog operationLog = new OperationLog();
            operationLog.setActionType(normalizeText(actionType));
            operationLog.setModuleName(normalizeText(moduleName));
            operationLog.setBizType(normalizeText(bizType));
            operationLog.setBizId(bizId);
            operationLog.setBizNo(normalizeText(bizNo));
            operationLog.setOperatorName(normalizeText(operatorName));
            operationLog.setResultStatus("SUCCESS");
            operationLog.setMessage(normalizeText(message));
            operationLogMapper.insert(operationLog);
        } catch (Exception ex) {
            log.warn("写入操作日志失败: actionType={}, bizType={}, bizId={}", actionType, bizType, bizId, ex);
        }
    }

    @Override
    public PageResult<OperationLogVO> getLogPage(String actionType,
                                                 String moduleName,
                                                 String operatorName,
                                                 String resultStatus,
                                                 String bizNo,
                                                 Integer pageNum,
                                                 Integer pageSize) {
        int currentPage = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int currentSize = (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, MAX_PAGE_SIZE);
        int offset = (currentPage - 1) * currentSize;

        String normalizedActionType = normalizeText(actionType);
        String normalizedModuleName = normalizeText(moduleName);
        String normalizedOperatorName = normalizeText(operatorName);
        String normalizedResultStatus = normalizeText(resultStatus);
        String normalizedBizNo = normalizeText(bizNo);

        Long total = operationLogMapper.countLog(
                normalizedActionType,
                normalizedModuleName,
                normalizedOperatorName,
                normalizedResultStatus,
                normalizedBizNo
        );
        List<OperationLogVO> records = operationLogMapper.selectLogPage(
                normalizedActionType,
                normalizedModuleName,
                normalizedOperatorName,
                normalizedResultStatus,
                normalizedBizNo,
                offset,
                currentSize
        );
        return new PageResult<>(total, currentPage, currentSize, records);
    }

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}

