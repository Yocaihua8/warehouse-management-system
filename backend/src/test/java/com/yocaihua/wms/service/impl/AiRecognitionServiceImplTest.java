package com.yocaihua.wms.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.entity.AiRecognitionRecord;
import com.yocaihua.wms.mapper.CustomerMapper;
import com.yocaihua.wms.mapper.InboundOrderItemMapper;
import com.yocaihua.wms.mapper.InboundOrderMapper;
import com.yocaihua.wms.mapper.OutboundOrderItemMapper;
import com.yocaihua.wms.mapper.OutboundOrderMapper;
import com.yocaihua.wms.mapper.StockMapper;
import com.yocaihua.wms.mapper.SupplierMapper;
import com.yocaihua.wms.service.OperationLogService;
import com.yocaihua.wms.service.StockFlowService;
import com.yocaihua.wms.service.ai.AiDraftPersistenceService;
import com.yocaihua.wms.service.ai.AiOrderAssemblerService;
import com.yocaihua.wms.service.ai.AiRecognitionRecordService;
import com.yocaihua.wms.service.ai.OcrAdapterService;
import com.yocaihua.wms.service.ai.ProductMatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiRecognitionServiceImplTest {

    @Mock
    private AiRecognitionRecordService aiRecognitionRecordService;
    @Mock
    private InboundOrderMapper inboundOrderMapper;
    @Mock
    private OutboundOrderMapper outboundOrderMapper;
    @Mock
    private StockMapper stockMapper;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private InboundOrderItemMapper inboundOrderItemMapper;
    @Mock
    private OutboundOrderItemMapper outboundOrderItemMapper;
    @Mock
    private OcrAdapterService ocrAdapterService;
    @Mock
    private AiOrderAssemblerService aiOrderAssemblerService;
    @Mock
    private ProductMatchService productMatchService;
    @Mock
    private AiDraftPersistenceService aiDraftPersistenceService;
    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private SupplierMapper supplierMapper;
    @Mock
    private StockFlowService stockFlowService;
    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private AiRecognitionServiceImpl aiRecognitionService;

    @Test
    void recognizeInbound_shouldMarkFailedAndThrowBusinessException_whenOcrFails() {
        AiRecognitionRecord record = new AiRecognitionRecord();
        record.setId(1000L);

        when(aiRecognitionRecordService.createPendingRecord(any(), any(), eq("tester"), eq("inbound"))).thenReturn(record);
        when(ocrAdapterService.recognizeInbound(any())).thenThrow(new RuntimeException("OCR服务不可用"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aiRecognitionService.recognizeInbound(null, "tester"));

        assertEquals("OCR服务不可用", exception.getMessage());
        verify(aiRecognitionRecordService).markFailed(eq(1000L), any(RuntimeException.class));
    }

    @Test
    void recognizeOutbound_shouldMarkFailedAndThrowBusinessException_whenOcrFails() {
        AiRecognitionRecord record = new AiRecognitionRecord();
        record.setId(2000L);

        when(aiRecognitionRecordService.createPendingRecord(any(), any(), eq("tester"), eq("outbound"))).thenReturn(record);
        when(ocrAdapterService.recognizeOutbound(any())).thenThrow(new RuntimeException("出库OCR超时"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aiRecognitionService.recognizeOutbound(null, "tester"));

        assertEquals("出库OCR超时", exception.getMessage());
        verify(aiRecognitionRecordService).markFailed(eq(2000L), any(RuntimeException.class));
    }
}
