package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.dto.PythonOcrItemDTO;
import com.yocaihua.wms.dto.PythonOcrRecognizeDataDTO;
import com.yocaihua.wms.dto.AiInboundConfirmDTO;
import com.yocaihua.wms.dto.AiInboundConfirmItemDTO;
import com.yocaihua.wms.dto.AiOutboundConfirmDTO;
import com.yocaihua.wms.dto.AiOutboundConfirmItemDTO;
import com.yocaihua.wms.entity.AiRecognitionItem;
import com.yocaihua.wms.entity.AiRecognitionRecord;
import com.yocaihua.wms.entity.Customer;
import com.yocaihua.wms.entity.InboundOrder;
import com.yocaihua.wms.entity.InboundOrderItem;
import com.yocaihua.wms.entity.OutboundOrder;
import com.yocaihua.wms.entity.OutboundOrderItem;
import com.yocaihua.wms.entity.Supplier;
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
import com.yocaihua.wms.service.ai.AiRecognitionValidationService;
import com.yocaihua.wms.service.ai.AiRecognitionVoAssemblerService;
import com.yocaihua.wms.service.ai.OcrAdapterService;
import com.yocaihua.wms.service.ai.ProductMatchService;
import com.yocaihua.wms.vo.AiInboundRecognizeItemVO;
import com.yocaihua.wms.vo.AiInboundRecognizeVO;
import com.yocaihua.wms.vo.AiOutboundRecognizeItemVO;
import com.yocaihua.wms.vo.AiOutboundRecognizeVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
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
    private InboundOrderItemMapper inboundOrderItemMapper;
    @Mock
    private OutboundOrderItemMapper outboundOrderItemMapper;
    @Mock
    private OcrAdapterService ocrAdapterService;
    @Mock
    private AiOrderAssemblerService aiOrderAssemblerService;
    @Mock
    private AiRecognitionValidationService aiRecognitionValidationService;
    @Mock
    private AiRecognitionVoAssemblerService aiRecognitionVoAssemblerService;
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

    @Test
    void recognizeInbound_shouldAppendItemsAndMarkSuccess_whenOcrSucceeds() {
        AiRecognitionRecord record = recognitionRecord(1100L, "AIR-IN-001", "inbound", "inbound.jpg");
        PythonOcrRecognizeDataDTO ocrData = inboundOcrData();
        List<AiInboundRecognizeItemVO> itemVOList = List.of(inboundItemVo());
        List<AiRecognitionItem> entityList = List.of(recognitionItem(1100L));
        ProductMatchService.SupplierMatchResult supplierMatchResult =
                new ProductMatchService.SupplierMatchResult(88L, "matched_exact", "按供应商名称精确匹配");
        AiInboundRecognizeVO resultVO = inboundResultVO(1100L);

        when(aiRecognitionRecordService.createPendingRecord(any(), eq("inbound.jpg"), eq("tester"), eq("inbound")))
                .thenReturn(record);
        when(ocrAdapterService.recognizeInbound(any())).thenReturn(ocrData);
        when(aiRecognitionVoAssemblerService.convertPythonItemsToInbound(ocrData.getItems())).thenReturn(itemVOList);
        when(productMatchService.findMatchedSupplier("供应商A")).thenReturn(supplierMatchResult);
        when(aiDraftPersistenceService.convertInboundItemsToEntities(1100L, itemVOList)).thenReturn(entityList);
        when(aiRecognitionVoAssemblerService.toJson(ocrData.getWarnings())).thenReturn("[\"数量疑似\"]");
        when(aiRecognitionVoAssemblerService.buildInboundRecognizeResult(
                record,
                "inbound.jpg",
                "供应商A",
                88L,
                "matched_exact",
                "原始OCR文本",
                "[\"数量疑似\"]",
                itemVOList
        )).thenReturn(resultVO);

        AiInboundRecognizeVO result = aiRecognitionService.recognizeInbound(null, "tester");

        assertSame(resultVO, result);
        verify(productMatchService).matchInboundProducts(itemVOList);
        verify(aiRecognitionRecordService).appendItems(entityList);
        verify(aiRecognitionRecordService).markInboundRecognizedSuccess(
                record,
                "供应商A",
                "原始OCR文本",
                "[\"数量疑似\"]",
                resultVO
        );
        verify(aiRecognitionRecordService, never()).markFailed(eq(1100L), any(Exception.class));
    }

    @Test
    void recognizeOutbound_shouldAppendItemsAndMarkSuccess_whenOcrSucceeds() {
        AiRecognitionRecord record = recognitionRecord(2200L, "AIR-OUT-001", "outbound", "outbound.jpg");
        PythonOcrRecognizeDataDTO ocrData = outboundOcrData();
        List<AiOutboundRecognizeItemVO> itemVOList = List.of(outboundItemVo());
        List<AiRecognitionItem> entityList = List.of(recognitionItem(2200L));
        ProductMatchService.CustomerMatchResult customerMatchResult =
                new ProductMatchService.CustomerMatchResult(66L, "matched_exact", "按客户名称精确匹配");
        AiOutboundRecognizeVO resultVO = outboundResultVO(2200L);

        when(aiRecognitionRecordService.createPendingRecord(any(), eq("outbound.jpg"), eq("tester"), eq("outbound")))
                .thenReturn(record);
        when(ocrAdapterService.recognizeOutbound(any())).thenReturn(ocrData);
        when(aiRecognitionVoAssemblerService.convertPythonItemsToOutbound(ocrData.getItems())).thenReturn(itemVOList);
        when(aiRecognitionVoAssemblerService.resolveRecognizedCustomerName(ocrData)).thenReturn("客户A");
        when(productMatchService.findMatchedCustomer("客户A")).thenReturn(customerMatchResult);
        when(aiDraftPersistenceService.convertOutboundItemsToEntities(2200L, itemVOList)).thenReturn(entityList);
        when(aiRecognitionVoAssemblerService.toJson(ocrData.getWarnings())).thenReturn("[\"客户模糊匹配\"]");
        when(aiRecognitionVoAssemblerService.buildOutboundRecognizeResult(
                record,
                "outbound.jpg",
                "客户A",
                66L,
                "matched_exact",
                "出库OCR文本",
                "[\"客户模糊匹配\"]",
                ocrData.getWarnings(),
                itemVOList
        )).thenReturn(resultVO);

        AiOutboundRecognizeVO result = aiRecognitionService.recognizeOutbound(null, "tester");

        assertSame(resultVO, result);
        verify(productMatchService).matchOutboundProducts(itemVOList);
        verify(aiRecognitionRecordService).appendItems(entityList);
        verify(aiRecognitionRecordService).markOutboundRecognizedSuccess(
                record,
                "客户A",
                "出库OCR文本",
                "[\"客户模糊匹配\"]",
                resultVO
        );
        verify(aiRecognitionRecordService, never()).markFailed(eq(2200L), any(Exception.class));
    }

    @Test
    void confirmInbound_shouldPersistOrderAndMarkConfirmed_whenValidationPasses() {
        AiInboundConfirmDTO dto = inboundConfirmDto();
        AiRecognitionRecord record = recognitionRecord(3100L, "AIR-IN-CONFIRM", "inbound", "draft-inbound.jpg");
        Supplier supplier = supplier(9L, "供应商A");
        List<AiInboundRecognizeItemVO> editedItemVOList = List.of(inboundItemVo());
        InboundOrder inboundOrder = new InboundOrder();
        inboundOrder.setOrderNo("RK20260414001");
        InboundOrderItem orderItem = inboundOrderItem(501L);

        when(aiRecognitionValidationService.getAndValidateInboundRecord(3100L)).thenReturn(record);
        when(supplierMapper.selectById(9L)).thenReturn(supplier);
        when(aiRecognitionVoAssemblerService.buildEditedInboundItemVOList(dto.getItemList())).thenReturn(editedItemVOList);
        when(aiOrderAssemblerService.buildInboundOrder(9L, "供应商A", "AI确认入库")).thenReturn(inboundOrder);
        when(aiOrderAssemblerService.resolveItemAmount(new BigDecimal("37.50"), 3, new BigDecimal("12.50")))
                .thenReturn(new BigDecimal("37.50"));
        when(aiOrderAssemblerService.buildInboundOrderItem(dto.getItemList().get(0), new BigDecimal("37.50")))
                .thenReturn(orderItem);
        doAnswer(invocation -> {
            InboundOrder saved = invocation.getArgument(0);
            saved.setId(9001L);
            return 1;
        }).when(inboundOrderMapper).insert(any(InboundOrder.class));
        when(aiRecognitionRecordService.markConfirmedToOrder(3100L, 9001L)).thenReturn(1);

        Long result = aiRecognitionService.confirmInbound(dto, "tester");

        assertEquals(9001L, result);
        verify(aiRecognitionValidationService).validateInboundConfirmDTO(dto);
        verify(aiRecognitionValidationService).validateInboundConfirmItem(dto.getItemList().get(0));
        verify(aiDraftPersistenceService).saveEditedInboundDraft(
                record,
                "供应商A",
                9L,
                "manual_confirmed",
                "入库原文",
                editedItemVOList
        );
        verify(inboundOrderMapper).insert(inboundOrder);
        verify(inboundOrderItemMapper).insertBatch(argThat(items ->
                items.size() == 1
                        && items.get(0).getInboundOrderId().equals(9001L)
                        && items.get(0).getProductId().equals(501L)
        ));
        verify(stockFlowService).increaseByInbound(
                9001L,
                "RK20260414001",
                List.of(orderItem),
                "tester",
                "AI_CONFIRM_INBOUND",
                "AI确认入库"
        );
        verify(operationLogService).recordSuccess(
                "AI_INBOUND_CONFIRM",
                "AI识别入库",
                "INBOUND_ORDER",
                9001L,
                "RK20260414001",
                "tester",
                "AI确认生成入库单，recordId=3100"
        );
    }

    @Test
    void confirmOutbound_shouldPersistOrderAndMarkConfirmed_whenValidationPasses() {
        AiOutboundConfirmDTO dto = outboundConfirmDto();
        AiRecognitionRecord record = recognitionRecord(3200L, "AIR-OUT-CONFIRM", "outbound", "draft-outbound.jpg");
        Customer customer = customer(6L, "客户A");
        List<AiOutboundRecognizeItemVO> editedItemVOList = List.of(outboundItemVo());
        OutboundOrder outboundOrder = new OutboundOrder();
        outboundOrder.setOrderNo("OUT20260414001");
        OutboundOrderItem orderItem = outboundOrderItem(601L);

        when(aiRecognitionValidationService.getAndValidateOutboundRecord(3200L)).thenReturn(record);
        when(customerMapper.selectById(6L)).thenReturn(customer);
        when(aiRecognitionVoAssemblerService.buildEditedOutboundItemVOList(dto.getItemList())).thenReturn(editedItemVOList);
        when(aiOrderAssemblerService.buildOutboundOrder(6L, "客户A", "AI确认出库")).thenReturn(outboundOrder);
        when(aiOrderAssemblerService.resolveItemAmount(new BigDecimal("36.00"), 2, new BigDecimal("18.00")))
                .thenReturn(new BigDecimal("36.00"));
        when(aiOrderAssemblerService.buildOutboundOrderItem(dto.getItemList().get(0), new BigDecimal("36.00")))
                .thenReturn(orderItem);
        doAnswer(invocation -> {
            OutboundOrder saved = invocation.getArgument(0);
            saved.setId(9002L);
            return 1;
        }).when(outboundOrderMapper).insert(any(OutboundOrder.class));
        when(outboundOrderItemMapper.insert(any(OutboundOrderItem.class))).thenReturn(1);
        when(aiRecognitionRecordService.markConfirmedToOrder(3200L, 9002L)).thenReturn(1);

        Long result = aiRecognitionService.confirmOutbound(dto, "tester");

        assertEquals(9002L, result);
        verify(aiRecognitionValidationService).validateOutboundConfirmDTO(dto);
        verify(aiRecognitionValidationService).validateOutboundConfirmItem(dto.getItemList().get(0));
        verify(aiDraftPersistenceService).saveEditedOutboundDraft(
                record,
                "客户A",
                "出库原文",
                6L,
                editedItemVOList
        );
        verify(outboundOrderMapper).insert(outboundOrder);
        verify(outboundOrderItemMapper).insert(argThat(item ->
                item.getOutboundOrderId().equals(9002L)
                        && item.getProductId().equals(601L)
        ));
        verify(stockFlowService).decreaseByOutbound(
                9002L,
                "OUT20260414001",
                List.of(orderItem),
                "tester",
                "AI_CONFIRM_OUTBOUND",
                "AI确认出库"
        );
        verify(operationLogService).recordSuccess(
                "AI_OUTBOUND_CONFIRM",
                "AI识别出库",
                "OUTBOUND_ORDER",
                9002L,
                "OUT20260414001",
                "tester",
                "AI确认生成出库单，recordId=3200"
        );
    }

    @Test
    void confirmInbound_shouldThrowAlreadyConfirmedException_whenMarkConfirmedToOrderReturnsZero() {
        AiInboundConfirmDTO dto = inboundConfirmDto();
        AiRecognitionRecord record = recognitionRecord(3300L, "AIR-IN-DUP", "inbound", "draft-inbound.jpg");
        Supplier supplier = supplier(9L, "供应商A");
        List<AiInboundRecognizeItemVO> editedItemVOList = List.of(inboundItemVo());
        InboundOrder inboundOrder = new InboundOrder();
        inboundOrder.setOrderNo("RK20260414002");
        InboundOrderItem orderItem = inboundOrderItem(501L);
        BusinessException expected = new BusinessException("该AI记录已确认生成入库单，不能重复确认");

        when(aiRecognitionValidationService.getAndValidateInboundRecord(3300L)).thenReturn(record);
        when(supplierMapper.selectById(9L)).thenReturn(supplier);
        when(aiRecognitionVoAssemblerService.buildEditedInboundItemVOList(dto.getItemList())).thenReturn(editedItemVOList);
        when(aiOrderAssemblerService.buildInboundOrder(9L, "供应商A", "AI确认入库")).thenReturn(inboundOrder);
        when(aiOrderAssemblerService.resolveItemAmount(new BigDecimal("37.50"), 3, new BigDecimal("12.50")))
                .thenReturn(new BigDecimal("37.50"));
        when(aiOrderAssemblerService.buildInboundOrderItem(dto.getItemList().get(0), new BigDecimal("37.50")))
                .thenReturn(orderItem);
        doAnswer(invocation -> {
            InboundOrder saved = invocation.getArgument(0);
            saved.setId(9101L);
            return 1;
        }).when(inboundOrderMapper).insert(any(InboundOrder.class));
        when(aiRecognitionRecordService.markConfirmedToOrder(3300L, 9101L)).thenReturn(0);
        when(aiRecognitionRecordService.getById(3300L)).thenReturn(record);
        when(aiRecognitionValidationService.buildInboundAlreadyConfirmedException(record)).thenReturn(expected);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aiRecognitionService.confirmInbound(inboundDtoWithRecordId(3300L), "tester"));

        assertSame(expected, exception);
        verify(aiRecognitionRecordService).getById(3300L);
        verify(aiRecognitionValidationService).buildInboundAlreadyConfirmedException(record);
        verify(stockFlowService).increaseByInbound(
                9101L,
                "RK20260414002",
                List.of(orderItem),
                "tester",
                "AI_CONFIRM_INBOUND",
                "AI确认入库"
        );
        verify(operationLogService, never()).recordSuccess(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmOutbound_shouldThrowAlreadyConfirmedException_whenMarkConfirmedToOrderReturnsZero() {
        AiOutboundConfirmDTO dto = outboundConfirmDto();
        AiRecognitionRecord record = recognitionRecord(3400L, "AIR-OUT-DUP", "outbound", "draft-outbound.jpg");
        Customer customer = customer(6L, "客户A");
        List<AiOutboundRecognizeItemVO> editedItemVOList = List.of(outboundItemVo());
        OutboundOrder outboundOrder = new OutboundOrder();
        outboundOrder.setOrderNo("OUT20260414002");
        OutboundOrderItem orderItem = outboundOrderItem(601L);
        BusinessException expected = new BusinessException("该AI记录已确认生成出库单，不能重复确认");

        when(aiRecognitionValidationService.getAndValidateOutboundRecord(3400L)).thenReturn(record);
        when(customerMapper.selectById(6L)).thenReturn(customer);
        when(aiRecognitionVoAssemblerService.buildEditedOutboundItemVOList(dto.getItemList())).thenReturn(editedItemVOList);
        when(aiOrderAssemblerService.buildOutboundOrder(6L, "客户A", "AI确认出库")).thenReturn(outboundOrder);
        when(aiOrderAssemblerService.resolveItemAmount(new BigDecimal("36.00"), 2, new BigDecimal("18.00")))
                .thenReturn(new BigDecimal("36.00"));
        when(aiOrderAssemblerService.buildOutboundOrderItem(dto.getItemList().get(0), new BigDecimal("36.00")))
                .thenReturn(orderItem);
        doAnswer(invocation -> {
            OutboundOrder saved = invocation.getArgument(0);
            saved.setId(9102L);
            return 1;
        }).when(outboundOrderMapper).insert(any(OutboundOrder.class));
        when(outboundOrderItemMapper.insert(any(OutboundOrderItem.class))).thenReturn(1);
        when(aiRecognitionRecordService.markConfirmedToOrder(3400L, 9102L)).thenReturn(0);
        when(aiRecognitionRecordService.getById(3400L)).thenReturn(record);
        when(aiRecognitionValidationService.buildOutboundAlreadyConfirmedException(record)).thenReturn(expected);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aiRecognitionService.confirmOutbound(outboundDtoWithRecordId(3400L), "tester"));

        assertSame(expected, exception);
        verify(aiRecognitionRecordService).getById(3400L);
        verify(aiRecognitionValidationService).buildOutboundAlreadyConfirmedException(record);
        verify(stockFlowService).decreaseByOutbound(
                9102L,
                "OUT20260414002",
                List.of(orderItem),
                "tester",
                "AI_CONFIRM_OUTBOUND",
                "AI确认出库"
        );
        verify(operationLogService, never()).recordSuccess(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmOutbound_shouldThrow_whenCustomerIsUnmatched() {
        AiOutboundConfirmDTO dto = outboundConfirmDtoWithoutCustomerId();
        dto.setRecordId(3500L);
        AiRecognitionRecord record = recognitionRecord(3500L, "AIR-OUT-NO-CUSTOMER", "outbound", "draft-outbound.jpg");

        when(aiRecognitionValidationService.getAndValidateOutboundRecord(3500L)).thenReturn(record);
        when(productMatchService.findMatchedCustomer("客户A"))
                .thenReturn(new ProductMatchService.CustomerMatchResult(null, "unmatched", "未匹配到系统客户"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aiRecognitionService.confirmOutbound(dto, "tester"));

        assertEquals("未匹配到客户，请先人工选择客户", exception.getMessage());
        verify(productMatchService).findMatchedCustomer("客户A");
        verify(outboundOrderMapper, never()).insert(any(OutboundOrder.class));
        verify(outboundOrderItemMapper, never()).insert(any(OutboundOrderItem.class));
        verify(stockFlowService, never()).decreaseByOutbound(any(), any(), any(), any(), any(), any());
        verify(operationLogService, never()).recordSuccess(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmOutbound_shouldThrow_whenInsertOrderItemFails() {
        AiOutboundConfirmDTO dto = outboundConfirmDto();
        AiRecognitionRecord record = recognitionRecord(3600L, "AIR-OUT-ITEM-FAIL", "outbound", "draft-outbound.jpg");
        Customer customer = customer(6L, "客户A");
        List<AiOutboundRecognizeItemVO> editedItemVOList = List.of(outboundItemVo());
        OutboundOrder outboundOrder = new OutboundOrder();
        outboundOrder.setOrderNo("OUT20260414003");
        OutboundOrderItem orderItem = outboundOrderItem(601L);

        when(aiRecognitionValidationService.getAndValidateOutboundRecord(3600L)).thenReturn(record);
        when(customerMapper.selectById(6L)).thenReturn(customer);
        when(aiRecognitionVoAssemblerService.buildEditedOutboundItemVOList(dto.getItemList())).thenReturn(editedItemVOList);
        when(aiOrderAssemblerService.buildOutboundOrder(6L, "客户A", "AI确认出库")).thenReturn(outboundOrder);
        when(aiOrderAssemblerService.resolveItemAmount(new BigDecimal("36.00"), 2, new BigDecimal("18.00")))
                .thenReturn(new BigDecimal("36.00"));
        when(aiOrderAssemblerService.buildOutboundOrderItem(dto.getItemList().get(0), new BigDecimal("36.00")))
                .thenReturn(orderItem);
        doAnswer(invocation -> {
            OutboundOrder saved = invocation.getArgument(0);
            saved.setId(9103L);
            return 1;
        }).when(outboundOrderMapper).insert(any(OutboundOrder.class));
        when(outboundOrderItemMapper.insert(any(OutboundOrderItem.class))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aiRecognitionService.confirmOutbound(outboundDtoWithRecordId(3600L), "tester"));

        assertEquals("保存出库单明细失败", exception.getMessage());
        verify(outboundOrderMapper).insert(outboundOrder);
        verify(outboundOrderItemMapper).insert(any(OutboundOrderItem.class));
        verify(stockFlowService, never()).decreaseByOutbound(any(), any(), any(), any(), any(), any());
        verify(operationLogService, never()).recordSuccess(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmOutbound_shouldThrow_whenCustomerDoesNotExist() {
        AiOutboundConfirmDTO dto = outboundConfirmDto();
        dto.setRecordId(3650L);
        AiRecognitionRecord record = recognitionRecord(3650L, "AIR-OUT-NO-CUSTOMER", "outbound", "draft-outbound.jpg");

        when(aiRecognitionValidationService.getAndValidateOutboundRecord(3650L)).thenReturn(record);
        when(customerMapper.selectById(6L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aiRecognitionService.confirmOutbound(dto, "tester"));

        assertEquals("客户不存在", exception.getMessage());
        verify(customerMapper).selectById(6L);
        verify(productMatchService, never()).findMatchedCustomer(any());
        verify(outboundOrderMapper, never()).insert(any(OutboundOrder.class));
        verify(outboundOrderItemMapper, never()).insert(any(OutboundOrderItem.class));
        verify(stockFlowService, never()).decreaseByOutbound(any(), any(), any(), any(), any(), any());
        verify(operationLogService, never()).recordSuccess(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmOutbound_shouldThrow_whenCustomerNameIsBlank() {
        AiOutboundConfirmDTO dto = outboundConfirmDtoWithoutCustomerId();
        dto.setRecordId(3660L);
        dto.setCustomerName("   ");
        AiRecognitionRecord record = recognitionRecord(3660L, "AIR-OUT-BLANK-CUSTOMER", "outbound", "draft-outbound.jpg");
        record.setSupplierName("   ");

        when(aiRecognitionValidationService.getAndValidateOutboundRecord(3660L)).thenReturn(record);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aiRecognitionService.confirmOutbound(dto, "tester"));

        assertEquals("客户名称不能为空", exception.getMessage());
        verify(productMatchService, never()).findMatchedCustomer(any());
        verify(outboundOrderMapper, never()).insert(any(OutboundOrder.class));
        verify(outboundOrderItemMapper, never()).insert(any(OutboundOrderItem.class));
        verify(stockFlowService, never()).decreaseByOutbound(any(), any(), any(), any(), any(), any());
        verify(operationLogService, never()).recordSuccess(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmOutbound_shouldThrow_whenMatchedCustomerDoesNotExist() {
        AiOutboundConfirmDTO dto = outboundConfirmDtoWithoutCustomerId();
        dto.setRecordId(3670L);
        AiRecognitionRecord record = recognitionRecord(3670L, "AIR-OUT-MATCHED-NO-CUSTOMER", "outbound", "draft-outbound.jpg");

        when(aiRecognitionValidationService.getAndValidateOutboundRecord(3670L)).thenReturn(record);
        when(productMatchService.findMatchedCustomer("客户A"))
                .thenReturn(new ProductMatchService.CustomerMatchResult(66L, "matched_exact", "按客户名称精确匹配"));
        when(customerMapper.selectById(66L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aiRecognitionService.confirmOutbound(dto, "tester"));

        assertEquals("客户不存在", exception.getMessage());
        verify(productMatchService).findMatchedCustomer("客户A");
        verify(customerMapper).selectById(66L);
        verify(outboundOrderMapper, never()).insert(any(OutboundOrder.class));
        verify(outboundOrderItemMapper, never()).insert(any(OutboundOrderItem.class));
        verify(stockFlowService, never()).decreaseByOutbound(any(), any(), any(), any(), any(), any());
        verify(operationLogService, never()).recordSuccess(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmOutbound_shouldThrow_whenResolvedCustomerDisplayNameIsBlank() {
        AiOutboundConfirmDTO dto = outboundConfirmDto();
        dto.setRecordId(3680L);
        dto.setCustomerName("   ");
        AiRecognitionRecord record = recognitionRecord(3680L, "AIR-OUT-BLANK-DISPLAY-NAME", "outbound", "draft-outbound.jpg");
        record.setSupplierName("   ");
        Customer customer = customer(6L, "   ");

        when(aiRecognitionValidationService.getAndValidateOutboundRecord(3680L)).thenReturn(record);
        when(customerMapper.selectById(6L)).thenReturn(customer);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aiRecognitionService.confirmOutbound(dto, "tester"));

        assertEquals("客户名称不能为空", exception.getMessage());
        verify(productMatchService, never()).findMatchedCustomer(any());
        verify(outboundOrderMapper, never()).insert(any(OutboundOrder.class));
        verify(outboundOrderItemMapper, never()).insert(any(OutboundOrderItem.class));
        verify(stockFlowService, never()).decreaseByOutbound(any(), any(), any(), any(), any(), any());
        verify(operationLogService, never()).recordSuccess(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmInbound_shouldThrow_whenSupplierDoesNotExist() {
        AiInboundConfirmDTO dto = inboundConfirmDto();
        dto.setRecordId(3700L);
        AiRecognitionRecord record = recognitionRecord(3700L, "AIR-IN-NO-SUPPLIER", "inbound", "draft-inbound.jpg");

        when(aiRecognitionValidationService.getAndValidateInboundRecord(3700L)).thenReturn(record);
        when(supplierMapper.selectById(9L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aiRecognitionService.confirmInbound(dto, "tester"));

        assertEquals("供应商不存在", exception.getMessage());
        verify(supplierMapper).selectById(9L);
        verify(inboundOrderMapper, never()).insert(any(InboundOrder.class));
        verify(inboundOrderItemMapper, never()).insertBatch(any());
        verify(stockFlowService, never()).increaseByInbound(any(), any(), any(), any(), any(), any());
        verify(operationLogService, never()).recordSuccess(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmInbound_shouldThrow_whenSupplierNameIsBlank() {
        AiInboundConfirmDTO dto = inboundConfirmDtoWithoutSupplierId();
        dto.setRecordId(3800L);
        AiRecognitionRecord record = recognitionRecord(3800L, "AIR-IN-BLANK-SUPPLIER", "inbound", "draft-inbound.jpg");
        record.setSupplierName("   ");

        when(aiRecognitionValidationService.getAndValidateInboundRecord(3800L)).thenReturn(record);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aiRecognitionService.confirmInbound(dto, "tester"));

        assertEquals("供应商名称不能为空", exception.getMessage());
        verify(inboundOrderMapper, never()).insert(any(InboundOrder.class));
        verify(inboundOrderItemMapper, never()).insertBatch(any());
        verify(stockFlowService, never()).increaseByInbound(any(), any(), any(), any(), any(), any());
        verify(operationLogService, never()).recordSuccess(any(), any(), any(), any(), any(), any(), any());
    }

    private AiRecognitionRecord recognitionRecord(Long id, String taskNo, String docType, String fileName) {
        AiRecognitionRecord record = new AiRecognitionRecord();
        record.setId(id);
        record.setTaskNo(taskNo);
        record.setDocType(docType);
        record.setSourceFileName(fileName);
        return record;
    }

    private PythonOcrRecognizeDataDTO inboundOcrData() {
        PythonOcrRecognizeDataDTO dto = new PythonOcrRecognizeDataDTO();
        dto.setSupplierName("供应商A");
        dto.setRawText("原始OCR文本");
        dto.setWarnings(List.of("数量疑似"));
        dto.setItems(List.of(pythonItemDto()));
        return dto;
    }

    private PythonOcrRecognizeDataDTO outboundOcrData() {
        PythonOcrRecognizeDataDTO dto = new PythonOcrRecognizeDataDTO();
        dto.setCustomerName("客户A");
        dto.setRawText("出库OCR文本");
        dto.setWarnings(List.of("客户模糊匹配"));
        dto.setItems(List.of(pythonItemDto()));
        return dto;
    }

    private PythonOcrItemDTO pythonItemDto() {
        PythonOcrItemDTO item = new PythonOcrItemDTO();
        item.setLineNo(1);
        item.setProductName("商品A");
        item.setSpecification("500g");
        item.setUnit("袋");
        item.setQuantity(3);
        item.setUnitPrice(new BigDecimal("12.50"));
        item.setAmount(new BigDecimal("37.50"));
        return item;
    }

    private AiInboundRecognizeItemVO inboundItemVo() {
        AiInboundRecognizeItemVO vo = new AiInboundRecognizeItemVO();
        vo.setLineNo(1);
        vo.setProductName("商品A");
        vo.setSpecification("500g");
        vo.setUnit("袋");
        vo.setQuantity(3);
        vo.setUnitPrice(new BigDecimal("12.50"));
        vo.setAmount(new BigDecimal("37.50"));
        vo.setMatchedProductId(101L);
        vo.setMatchStatus("matched_exact");
        vo.setRemark("python识别");
        return vo;
    }

    private AiOutboundRecognizeItemVO outboundItemVo() {
        AiOutboundRecognizeItemVO vo = new AiOutboundRecognizeItemVO();
        vo.setLineNo(1);
        vo.setProductName("商品A");
        vo.setSpecification("500g");
        vo.setUnit("袋");
        vo.setQuantity(2);
        vo.setUnitPrice(new BigDecimal("18.00"));
        vo.setAmount(new BigDecimal("36.00"));
        vo.setMatchedProductId(102L);
        vo.setMatchStatus("matched_exact");
        vo.setRemark("python识别");
        return vo;
    }

    private AiRecognitionItem recognitionItem(Long recordId) {
        AiRecognitionItem item = new AiRecognitionItem();
        item.setRecordId(recordId);
        item.setLineNo(1);
        item.setProductName("商品A");
        return item;
    }

    private AiInboundRecognizeVO inboundResultVO(Long recordId) {
        AiInboundRecognizeVO vo = new AiInboundRecognizeVO();
        vo.setRecordId(recordId);
        vo.setSupplierName("供应商A");
        vo.setRecognitionStatus("success");
        return vo;
    }

    private AiOutboundRecognizeVO outboundResultVO(Long recordId) {
        AiOutboundRecognizeVO vo = new AiOutboundRecognizeVO();
        vo.setRecordId(recordId);
        vo.setCustomerName("客户A");
        vo.setRecognitionStatus("success");
        return vo;
    }

    private AiInboundConfirmDTO inboundConfirmDto() {
        AiInboundConfirmItemDTO item = new AiInboundConfirmItemDTO();
        item.setLineNo(1);
        item.setProductName("商品A");
        item.setSpecification("500g");
        item.setUnit("袋");
        item.setMatchedProductId(501L);
        item.setQuantity(3);
        item.setUnitPrice(new BigDecimal("12.50"));
        item.setAmount(new BigDecimal("37.50"));
        item.setRemark("首行");

        AiInboundConfirmDTO dto = new AiInboundConfirmDTO();
        dto.setRecordId(3100L);
        dto.setSupplierId(9L);
        dto.setRawText("入库原文");
        dto.setRemark("AI确认入库");
        dto.setItemList(List.of(item));
        return dto;
    }

    private AiInboundConfirmDTO inboundConfirmDtoWithoutSupplierId() {
        AiInboundConfirmDTO dto = inboundConfirmDto();
        dto.setSupplierId(null);
        dto.setSupplierName("   ");
        return dto;
    }

    private AiOutboundConfirmDTO outboundConfirmDto() {
        AiOutboundConfirmItemDTO item = new AiOutboundConfirmItemDTO();
        item.setLineNo(1);
        item.setProductName("商品A");
        item.setSpecification("500g");
        item.setUnit("袋");
        item.setMatchedProductId(601L);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("18.00"));
        item.setAmount(new BigDecimal("36.00"));
        item.setRemark("首行");

        AiOutboundConfirmDTO dto = new AiOutboundConfirmDTO();
        dto.setRecordId(3200L);
        dto.setCustomerId(6L);
        dto.setRawText("出库原文");
        dto.setRemark("AI确认出库");
        dto.setItemList(List.of(item));
        return dto;
    }

    private AiOutboundConfirmDTO outboundConfirmDtoWithoutCustomerId() {
        AiOutboundConfirmDTO dto = outboundConfirmDto();
        dto.setCustomerId(null);
        dto.setCustomerName("客户A");
        return dto;
    }

    private AiInboundConfirmDTO inboundDtoWithRecordId(Long recordId) {
        AiInboundConfirmDTO dto = inboundConfirmDto();
        dto.setRecordId(recordId);
        return dto;
    }

    private AiOutboundConfirmDTO outboundDtoWithRecordId(Long recordId) {
        AiOutboundConfirmDTO dto = outboundConfirmDto();
        dto.setRecordId(recordId);
        return dto;
    }


    private Supplier supplier(Long id, String name) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setSupplierName(name);
        return supplier;
    }

    private Customer customer(Long id, String name) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setCustomerName(name);
        return customer;
    }

    private InboundOrderItem inboundOrderItem(Long productId) {
        InboundOrderItem item = new InboundOrderItem();
        item.setProductId(productId);
        item.setQuantity(3);
        item.setUnitPrice(new BigDecimal("12.50"));
        item.setAmount(new BigDecimal("37.50"));
        return item;
    }

    private OutboundOrderItem outboundOrderItem(Long productId) {
        OutboundOrderItem item = new OutboundOrderItem();
        item.setProductId(productId);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("18.00"));
        item.setAmount(new BigDecimal("36.00"));
        return item;
    }
}
