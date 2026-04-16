package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.entity.OperationLog;
import com.yocaihua.wms.mapper.OperationLogMapper;
import com.yocaihua.wms.vo.OperationLogVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationLogServiceImplTest {

    @Mock
    private OperationLogMapper operationLogMapper;

    @InjectMocks
    private OperationLogServiceImpl operationLogService;

    @Test
    void recordSuccess_shouldNormalizeFieldsAndPersistSuccessLog() {
        operationLogService.recordSuccess(
                "  LOGIN_SUCCESS  ",
                "  用户认证  ",
                "  USER  ",
                10L,
                "  NO-001  ",
                "  tester  ",
                "  登录成功  "
        );

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogMapper).insert(captor.capture());

        OperationLog saved = captor.getValue();
        assertEquals("LOGIN_SUCCESS", saved.getActionType());
        assertEquals("用户认证", saved.getModuleName());
        assertEquals("USER", saved.getBizType());
        assertEquals(10L, saved.getBizId());
        assertEquals("NO-001", saved.getBizNo());
        assertEquals("tester", saved.getOperatorName());
        assertEquals("SUCCESS", saved.getResultStatus());
        assertEquals("登录成功", saved.getMessage());
    }

    @Test
    void recordSuccess_shouldConvertBlankStringsToNull() {
        operationLogService.recordSuccess("   ", " ", "", 20L, "   ", " ", "  ");

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogMapper).insert(captor.capture());

        OperationLog saved = captor.getValue();
        assertEquals(null, saved.getActionType());
        assertEquals(null, saved.getModuleName());
        assertEquals(null, saved.getBizType());
        assertEquals(20L, saved.getBizId());
        assertEquals(null, saved.getBizNo());
        assertEquals(null, saved.getOperatorName());
        assertEquals("SUCCESS", saved.getResultStatus());
        assertEquals(null, saved.getMessage());
    }

    @Test
    void recordSuccess_shouldNotThrow_whenInsertFails() {
        doThrow(new IllegalStateException("db down")).when(operationLogMapper).insert(any(OperationLog.class));

        assertDoesNotThrow(() -> operationLogService.recordSuccess(
                "LOGIN_SUCCESS",
                "用户认证",
                "USER",
                10L,
                null,
                "tester",
                "登录成功"
        ));
    }

    @Test
    void getLogPage_shouldUseDefaultPagination_whenPageParamsInvalid() {
        OperationLogVO vo = logVo(1L, "LOGIN_SUCCESS", "tester");
        when(operationLogMapper.countLog("LOGIN_SUCCESS", "用户认证", "tester", "SUCCESS", "NO-001")).thenReturn(1L);
        when(operationLogMapper.selectLogPage("LOGIN_SUCCESS", "用户认证", "tester", "SUCCESS", "NO-001", 0, 10))
                .thenReturn(List.of(vo));

        PageResult<OperationLogVO> result = operationLogService.getLogPage(
                "  LOGIN_SUCCESS  ",
                "  用户认证  ",
                "  tester  ",
                "  SUCCESS  ",
                "  NO-001  ",
                0,
                0
        );

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getPageNum());
        assertEquals(10, result.getPageSize());
        assertEquals(1, result.getList().size());
        verify(operationLogMapper).countLog("LOGIN_SUCCESS", "用户认证", "tester", "SUCCESS", "NO-001");
        verify(operationLogMapper).selectLogPage("LOGIN_SUCCESS", "用户认证", "tester", "SUCCESS", "NO-001", 0, 10);
    }

    @Test
    void getLogPage_shouldClampPageSizeAndNormalizeBlankFilters() {
        when(operationLogMapper.countLog(null, null, null, null, null)).thenReturn(0L);
        when(operationLogMapper.selectLogPage(null, null, null, null, null, 0, 200)).thenReturn(List.of());

        PageResult<OperationLogVO> result = operationLogService.getLogPage(" ", " ", " ", " ", " ", 1, 999);

        assertEquals(0L, result.getTotal());
        assertEquals(1, result.getPageNum());
        assertEquals(200, result.getPageSize());
        assertEquals(0, result.getList().size());
        verify(operationLogMapper).countLog(null, null, null, null, null);
        verify(operationLogMapper).selectLogPage(null, null, null, null, null, 0, 200);
    }

    private OperationLogVO logVo(Long id, String actionType, String operatorName) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(id);
        vo.setActionType(actionType);
        vo.setOperatorName(operatorName);
        return vo;
    }
}
