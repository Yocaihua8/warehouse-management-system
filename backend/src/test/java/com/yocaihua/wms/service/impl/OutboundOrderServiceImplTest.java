package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.CurrentUserContext;
import com.yocaihua.wms.common.OrderStatusConstant;
import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.StockChangeTypeConstant;
import com.yocaihua.wms.dto.OutboundOrderAddDTO;
import com.yocaihua.wms.dto.OutboundOrderItemAddDTO;
import com.yocaihua.wms.entity.Customer;
import com.yocaihua.wms.entity.OutboundOrder;
import com.yocaihua.wms.entity.OutboundOrderItem;
import com.yocaihua.wms.entity.Product;
import com.yocaihua.wms.mapper.CustomerMapper;
import com.yocaihua.wms.mapper.OutboundOrderItemMapper;
import com.yocaihua.wms.mapper.OutboundOrderMapper;
import com.yocaihua.wms.mapper.ProductMapper;
import com.yocaihua.wms.mapper.StockMapper;
import com.yocaihua.wms.service.OperationLogService;
import com.yocaihua.wms.service.StockFlowService;
import com.yocaihua.wms.vo.OrderCreatedVO;
import com.yocaihua.wms.vo.OutboundOrderDetailVO;
import com.yocaihua.wms.vo.OutboundOrderItemVO;
import com.yocaihua.wms.vo.OutboundOrderVO;
import com.yocaihua.wms.vo.StockVO;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboundOrderServiceImplTest {

    @Mock
    private OutboundOrderMapper outboundOrderMapper;
    @Mock
    private OutboundOrderItemMapper outboundOrderItemMapper;
    @Mock
    private StockMapper stockMapper;
    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private StockFlowService stockFlowService;
    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private OutboundOrderServiceImpl outboundOrderService;

    @BeforeEach
    void setUp() {
        CurrentUserContext.clear();
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void saveOutboundOrder_shouldPersistOrderAndItems_whenValid() {
        OutboundOrderAddDTO dto = new OutboundOrderAddDTO();
        dto.setCustomerId(66L);
        dto.setRemark("首单");
        dto.setItemList(List.of(
                createItemDto(10L, 2, "12.50", "第一行"),
                createItemDto(11L, 1, "10.50", "第二行")
        ));

        when(customerMapper.selectById(66L)).thenReturn(createCustomer(66L, "客户A", 1));
        when(productMapper.selectById(10L)).thenReturn(createProduct(10L, "商品A", "500g", "袋"));
        when(productMapper.selectById(11L)).thenReturn(createProduct(11L, "商品B", "1L", "瓶"));
        when(stockMapper.selectByProductId(10L)).thenReturn(createStock(10L, 100));
        when(stockMapper.selectByProductId(11L)).thenReturn(createStock(11L, 60));
        when(outboundOrderMapper.insert(any(OutboundOrder.class))).thenAnswer(invocation -> {
            OutboundOrder order = invocation.getArgument(0);
            order.setId(300L);
            return 1;
        });
        when(outboundOrderItemMapper.insert(any(OutboundOrderItem.class))).thenReturn(1);

        OrderCreatedVO result = outboundOrderService.saveOutboundOrder(dto);

        assertEquals(300L, result.getId());
        assertTrue(result.getOrderNo().startsWith("OUT"));
        verify(outboundOrderMapper).insert(argThat(order ->
                order.getCustomerId().equals(66L)
                        && "客户A".equals(order.getCustomerNameSnapshot())
                        && "首单".equals(order.getRemark())
                        && OrderStatusConstant.OUTBOUND_DRAFT.equals(order.getOrderStatus())
                        && order.getOrderNo() != null
                        && order.getOrderNo().startsWith("OUT")
                        && order.getCreatedTime() != null
                        && order.getTotalAmount().compareTo(new BigDecimal("35.50")) == 0
        ));

        ArgumentCaptor<OutboundOrderItem> itemCaptor = ArgumentCaptor.forClass(OutboundOrderItem.class);
        verify(outboundOrderItemMapper, times(2)).insert(itemCaptor.capture());
        List<OutboundOrderItem> savedItems = itemCaptor.getAllValues();
        assertEquals("商品A", savedItems.get(0).getProductNameSnapshot());
        assertEquals("袋", savedItems.get(0).getUnitSnapshot());
        assertEquals(0, savedItems.get(0).getAmount().compareTo(new BigDecimal("25.00")));
    }

    @Test
    void saveOutboundOrder_shouldThrow_whenItemListIsEmpty() {
        OutboundOrderAddDTO dto = new OutboundOrderAddDTO();
        dto.setCustomerId(66L);
        dto.setItemList(List.of());

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.saveOutboundOrder(dto));

        assertEquals("出库明细不能为空", exception.getMessage());
        verify(customerMapper, never()).selectById(any());
    }

    @Test
    void saveOutboundOrder_shouldThrow_whenCustomerDoesNotExist() {
        when(customerMapper.selectById(66L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.saveOutboundOrder(buildSingleItemOrderDto()));

        assertEquals("客户不存在", exception.getMessage());
        verify(productMapper, never()).selectById(any());
    }

    @Test
    void saveOutboundOrder_shouldThrow_whenCustomerIsDisabled() {
        when(customerMapper.selectById(66L)).thenReturn(createCustomer(66L, "客户A", 0));

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.saveOutboundOrder(buildSingleItemOrderDto()));

        assertEquals("客户已停用，不能用于新出库单", exception.getMessage());
        verify(productMapper, never()).selectById(any());
    }

    @Test
    void saveOutboundOrder_shouldThrow_whenDuplicateProductExists() {
        OutboundOrderAddDTO dto = new OutboundOrderAddDTO();
        dto.setCustomerId(66L);
        dto.setItemList(List.of(
                createItemDto(10L, 2, "12.50", "第一行"),
                createItemDto(10L, 1, "10.50", "第二行")
        ));

        when(customerMapper.selectById(66L)).thenReturn(createCustomer(66L, "客户A", 1));
        when(productMapper.selectById(10L)).thenReturn(createProduct(10L, "商品A", "500g", "袋"));
        when(stockMapper.selectByProductId(10L)).thenReturn(createStock(10L, 100));

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.saveOutboundOrder(dto));

        assertEquals("同一商品不能重复出现在出库单中", exception.getMessage());
        verify(outboundOrderMapper, never()).insert(any(OutboundOrder.class));
    }

    @Test
    void saveOutboundOrder_shouldThrow_whenProductDoesNotExist() {
        when(customerMapper.selectById(66L)).thenReturn(createCustomer(66L, "客户A", 1));
        when(productMapper.selectById(10L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.saveOutboundOrder(buildSingleItemOrderDto()));

        assertEquals("商品不存在，productId=10", exception.getMessage());
        verify(stockMapper, never()).selectByProductId(any());
    }

    @Test
    void saveOutboundOrder_shouldThrow_whenStockRecordDoesNotExist() {
        when(customerMapper.selectById(66L)).thenReturn(createCustomer(66L, "客户A", 1));
        when(productMapper.selectById(10L)).thenReturn(createProduct(10L, "商品A", "500g", "袋"));
        when(stockMapper.selectByProductId(10L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.saveOutboundOrder(buildSingleItemOrderDto()));

        assertEquals("商品库存记录不存在，productId=10", exception.getMessage());
        verify(outboundOrderMapper, never()).insert(any(OutboundOrder.class));
    }

    @Test
    void saveOutboundOrder_shouldThrow_whenStockInsufficient() {
        when(customerMapper.selectById(66L)).thenReturn(createCustomer(66L, "客户A", 1));
        when(productMapper.selectById(10L)).thenReturn(createProduct(10L, "商品A", "500g", "袋"));
        when(stockMapper.selectByProductId(10L)).thenReturn(createStock(10L, 1));

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.saveOutboundOrder(buildSingleItemOrderDto()));

        assertEquals("商品库存不足，productId=10", exception.getMessage());
    }

    @Test
    void saveOutboundOrder_shouldThrow_whenOrderInsertFails() {
        when(customerMapper.selectById(66L)).thenReturn(createCustomer(66L, "客户A", 1));
        when(productMapper.selectById(10L)).thenReturn(createProduct(10L, "商品A", "500g", "袋"));
        when(stockMapper.selectByProductId(10L)).thenReturn(createStock(10L, 100));
        when(outboundOrderMapper.insert(any(OutboundOrder.class))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.saveOutboundOrder(buildSingleItemOrderDto()));

        assertEquals("保存出库单失败", exception.getMessage());
        verify(outboundOrderItemMapper, never()).insert(any(OutboundOrderItem.class));
    }

    @Test
    void saveOutboundOrder_shouldThrow_whenInsertOrderItemFails() {
        when(customerMapper.selectById(66L)).thenReturn(createCustomer(66L, "客户A", 1));
        when(productMapper.selectById(10L)).thenReturn(createProduct(10L, "商品A", "500g", "袋"));
        when(stockMapper.selectByProductId(10L)).thenReturn(createStock(10L, 100));
        when(outboundOrderMapper.insert(any(OutboundOrder.class))).thenAnswer(invocation -> {
            OutboundOrder order = invocation.getArgument(0);
            order.setId(300L);
            return 1;
        });
        when(outboundOrderItemMapper.insert(any(OutboundOrderItem.class))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.saveOutboundOrder(buildSingleItemOrderDto()));

        assertEquals("保存出库单明细失败", exception.getMessage());
    }

    @Test
    void updateOutboundOrderDraft_shouldReplaceItemsAndUpdateHeader_whenDraft() {
        OutboundOrder existing = buildOutboundOrder(500L, "OUT500", OrderStatusConstant.OUTBOUND_DRAFT, "旧备注");
        OutboundOrderAddDTO dto = buildSingleItemOrderDto();
        dto.setRemark("更新出库草稿");

        when(outboundOrderMapper.selectById(500L)).thenReturn(existing);
        when(customerMapper.selectById(66L)).thenReturn(createCustomer(66L, "客户A", 1));
        when(productMapper.selectById(10L)).thenReturn(createProduct(10L, "商品A", "500g", "袋"));
        when(stockMapper.selectByProductId(10L)).thenReturn(createStock(10L, 100));
        when(outboundOrderMapper.updateDraftById(eq(500L), eq(66L), eq("客户A"), eq(new BigDecimal("25.00")), eq("更新出库草稿"), eq(OrderStatusConstant.OUTBOUND_DRAFT))).thenReturn(1);
        when(outboundOrderItemMapper.insert(any(OutboundOrderItem.class))).thenReturn(1);

        String result = outboundOrderService.updateOutboundOrderDraft(500L, dto);

        assertEquals("更新出库草稿成功", result);
        verify(outboundOrderItemMapper).deleteByOutboundOrderId(500L);
        verify(outboundOrderItemMapper).insert(any(OutboundOrderItem.class));
    }

    @Test
    void updateOutboundOrderDraft_shouldThrow_whenOrderDoesNotExist() {
        when(outboundOrderMapper.selectById(500L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.updateOutboundOrderDraft(500L, buildSingleItemOrderDto()));

        assertEquals("出库单不存在", exception.getMessage());
    }

    @Test
    void updateOutboundOrderDraft_shouldThrow_whenOrderIsNotDraft() {
        when(outboundOrderMapper.selectById(500L)).thenReturn(buildOutboundOrder(500L, "OUT500", OrderStatusConstant.OUTBOUND_COMPLETED, "done"));

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.updateOutboundOrderDraft(500L, buildSingleItemOrderDto()));

        assertEquals("仅草稿状态出库单允许编辑", exception.getMessage());
    }

    @Test
    void updateOutboundOrderDraft_shouldThrow_whenItemListIsEmpty() {
        OutboundOrderAddDTO dto = new OutboundOrderAddDTO();
        dto.setCustomerId(66L);
        dto.setItemList(List.of());
        when(outboundOrderMapper.selectById(500L)).thenReturn(buildOutboundOrder(500L, "OUT500", OrderStatusConstant.OUTBOUND_DRAFT, "draft"));

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.updateOutboundOrderDraft(500L, dto));

        assertEquals("出库明细不能为空", exception.getMessage());
    }

    @Test
    void updateOutboundOrderDraft_shouldThrow_whenCustomerDoesNotExist() {
        when(outboundOrderMapper.selectById(500L)).thenReturn(buildOutboundOrder(500L, "OUT500", OrderStatusConstant.OUTBOUND_DRAFT, "draft"));
        when(customerMapper.selectById(66L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.updateOutboundOrderDraft(500L, buildSingleItemOrderDto()));

        assertEquals("客户不存在", exception.getMessage());
    }

    @Test
    void updateOutboundOrderDraft_shouldThrow_whenCustomerIsDisabled() {
        when(outboundOrderMapper.selectById(500L)).thenReturn(buildOutboundOrder(500L, "OUT500", OrderStatusConstant.OUTBOUND_DRAFT, "draft"));
        when(customerMapper.selectById(66L)).thenReturn(createCustomer(66L, "客户A", 0));

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.updateOutboundOrderDraft(500L, buildSingleItemOrderDto()));

        assertEquals("客户已停用，不能用于新出库单", exception.getMessage());
    }

    @Test
    void updateOutboundOrderDraft_shouldThrow_whenDuplicateProductExists() {
        OutboundOrderAddDTO dto = new OutboundOrderAddDTO();
        dto.setCustomerId(66L);
        dto.setItemList(List.of(
                createItemDto(10L, 2, "12.50", "第一行"),
                createItemDto(10L, 1, "10.50", "第二行")
        ));

        when(outboundOrderMapper.selectById(500L)).thenReturn(buildOutboundOrder(500L, "OUT500", OrderStatusConstant.OUTBOUND_DRAFT, "draft"));
        when(customerMapper.selectById(66L)).thenReturn(createCustomer(66L, "客户A", 1));
        when(productMapper.selectById(10L)).thenReturn(createProduct(10L, "商品A", "500g", "袋"));
        when(stockMapper.selectByProductId(10L)).thenReturn(createStock(10L, 100));

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.updateOutboundOrderDraft(500L, dto));

        assertEquals("同一商品不能重复出现在出库单中", exception.getMessage());
    }

    @Test
    void updateOutboundOrderDraft_shouldThrow_whenProductDoesNotExist() {
        when(outboundOrderMapper.selectById(500L)).thenReturn(buildOutboundOrder(500L, "OUT500", OrderStatusConstant.OUTBOUND_DRAFT, "draft"));
        when(customerMapper.selectById(66L)).thenReturn(createCustomer(66L, "客户A", 1));
        when(productMapper.selectById(10L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.updateOutboundOrderDraft(500L, buildSingleItemOrderDto()));

        assertEquals("商品不存在，productId=10", exception.getMessage());
    }

    @Test
    void updateOutboundOrderDraft_shouldThrow_whenStockRecordDoesNotExist() {
        when(outboundOrderMapper.selectById(500L)).thenReturn(buildOutboundOrder(500L, "OUT500", OrderStatusConstant.OUTBOUND_DRAFT, "draft"));
        when(customerMapper.selectById(66L)).thenReturn(createCustomer(66L, "客户A", 1));
        when(productMapper.selectById(10L)).thenReturn(createProduct(10L, "商品A", "500g", "袋"));
        when(stockMapper.selectByProductId(10L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.updateOutboundOrderDraft(500L, buildSingleItemOrderDto()));

        assertEquals("商品库存记录不存在，productId=10", exception.getMessage());
    }

    @Test
    void updateOutboundOrderDraft_shouldThrow_whenStockInsufficient() {
        when(outboundOrderMapper.selectById(500L)).thenReturn(buildOutboundOrder(500L, "OUT500", OrderStatusConstant.OUTBOUND_DRAFT, "draft"));
        when(customerMapper.selectById(66L)).thenReturn(createCustomer(66L, "客户A", 1));
        when(productMapper.selectById(10L)).thenReturn(createProduct(10L, "商品A", "500g", "袋"));
        when(stockMapper.selectByProductId(10L)).thenReturn(createStock(10L, 1));

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.updateOutboundOrderDraft(500L, buildSingleItemOrderDto()));

        assertEquals("商品库存不足，productId=10", exception.getMessage());
    }

    @Test
    void updateOutboundOrderDraft_shouldThrow_whenUpdateHeaderFails() {
        when(outboundOrderMapper.selectById(500L)).thenReturn(buildOutboundOrder(500L, "OUT500", OrderStatusConstant.OUTBOUND_DRAFT, "draft"));
        when(customerMapper.selectById(66L)).thenReturn(createCustomer(66L, "客户A", 1));
        when(productMapper.selectById(10L)).thenReturn(createProduct(10L, "商品A", "500g", "袋"));
        when(stockMapper.selectByProductId(10L)).thenReturn(createStock(10L, 100));
        when(outboundOrderMapper.updateDraftById(eq(500L), eq(66L), eq("客户A"), eq(new BigDecimal("25.00")), eq("首单"), eq(OrderStatusConstant.OUTBOUND_DRAFT))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.updateOutboundOrderDraft(500L, buildSingleItemOrderDto()));

        assertEquals("出库单状态已变化，请刷新后重试", exception.getMessage());
        verify(outboundOrderItemMapper, never()).deleteByOutboundOrderId(any());
    }

    @Test
    void updateOutboundOrderDraft_shouldThrow_whenInsertOrderItemFails() {
        when(outboundOrderMapper.selectById(500L)).thenReturn(buildOutboundOrder(500L, "OUT500", OrderStatusConstant.OUTBOUND_DRAFT, "draft"));
        when(customerMapper.selectById(66L)).thenReturn(createCustomer(66L, "客户A", 1));
        when(productMapper.selectById(10L)).thenReturn(createProduct(10L, "商品A", "500g", "袋"));
        when(stockMapper.selectByProductId(10L)).thenReturn(createStock(10L, 100));
        when(outboundOrderMapper.updateDraftById(eq(500L), eq(66L), eq("客户A"), eq(new BigDecimal("25.00")), eq("首单"), eq(OrderStatusConstant.OUTBOUND_DRAFT))).thenReturn(1);
        when(outboundOrderItemMapper.insert(any(OutboundOrderItem.class))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.updateOutboundOrderDraft(500L, buildSingleItemOrderDto()));

        assertEquals("保存出库单明细失败", exception.getMessage());
        verify(outboundOrderItemMapper).deleteByOutboundOrderId(500L);
    }

    @Test
    void confirmOutboundOrder_shouldDecreaseStockAndComplete_whenDraft() {
        CurrentUserContext.setRole("ADMIN");
        CurrentUserContext.setUsername("tester");

        OutboundOrder order = buildOutboundOrder(300L, "OUT300", OrderStatusConstant.OUTBOUND_DRAFT, "manual outbound");
        OutboundOrderItem item = new OutboundOrderItem();
        item.setOutboundOrderId(300L);
        item.setProductId(3L);
        item.setQuantity(6);

        when(outboundOrderMapper.selectById(300L)).thenReturn(order);
        when(outboundOrderItemMapper.selectEntityListByOutboundOrderId(300L)).thenReturn(List.of(item));
        when(outboundOrderMapper.updateStatus(300L, OrderStatusConstant.OUTBOUND_COMPLETED, OrderStatusConstant.OUTBOUND_DRAFT)).thenReturn(1);

        String result = outboundOrderService.confirmOutboundOrder(300L);

        assertEquals("确认出库成功", result);
        verify(stockFlowService).decreaseByOutbound(eq(300L), eq("OUT300"), any(List.class), eq("tester"), eq(StockChangeTypeConstant.MANUAL_OUTBOUND), eq("manual outbound"));
        verify(outboundOrderMapper).updateStatus(300L, OrderStatusConstant.OUTBOUND_COMPLETED, OrderStatusConstant.OUTBOUND_DRAFT);
    }

    @Test
    void confirmOutboundOrder_shouldThrow_whenNotAdmin() {
        CurrentUserContext.setRole("OPERATOR");
        CurrentUserContext.setUsername("operator");

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.confirmOutboundOrder(300L));

        assertEquals("仅管理员可执行：确认出库", exception.getMessage());
        verify(outboundOrderMapper, never()).selectById(any());
    }

    @Test
    void confirmOutboundOrder_shouldThrow_whenOrderDoesNotExist() {
        CurrentUserContext.setRole("ADMIN");
        when(outboundOrderMapper.selectById(300L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.confirmOutboundOrder(300L));

        assertEquals("出库单不存在", exception.getMessage());
    }

    @Test
    void confirmOutboundOrder_shouldThrow_whenAlreadyCompleted() {
        CurrentUserContext.setRole("ADMIN");
        when(outboundOrderMapper.selectById(301L)).thenReturn(buildOutboundOrder(301L, "OUT301", OrderStatusConstant.OUTBOUND_COMPLETED, "done"));

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.confirmOutboundOrder(301L));

        assertEquals("出库单已确认出库，请勿重复操作", exception.getMessage());
        verify(stockFlowService, never()).decreaseByOutbound(any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmOutboundOrder_shouldThrow_whenOrderIsVoid() {
        CurrentUserContext.setRole("ADMIN");
        when(outboundOrderMapper.selectById(302L)).thenReturn(buildOutboundOrder(302L, "OUT302", OrderStatusConstant.OUTBOUND_VOID, "void"));

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.confirmOutboundOrder(302L));

        assertEquals("已作废出库单不能确认出库", exception.getMessage());
    }

    @Test
    void confirmOutboundOrder_shouldThrow_whenItemListIsEmpty() {
        CurrentUserContext.setRole("ADMIN");
        when(outboundOrderMapper.selectById(303L)).thenReturn(buildOutboundOrder(303L, "OUT303", OrderStatusConstant.OUTBOUND_DRAFT, "draft"));
        when(outboundOrderItemMapper.selectEntityListByOutboundOrderId(303L)).thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.confirmOutboundOrder(303L));

        assertEquals("出库单明细不能为空", exception.getMessage());
    }

    @Test
    void confirmOutboundOrder_shouldThrow_whenUpdateStatusFails() {
        CurrentUserContext.setRole("ADMIN");
        CurrentUserContext.setUsername("tester");

        OutboundOrder order = buildOutboundOrder(304L, "OUT304", OrderStatusConstant.OUTBOUND_DRAFT, "draft");
        OutboundOrderItem item = new OutboundOrderItem();
        item.setOutboundOrderId(304L);
        item.setProductId(10L);
        item.setQuantity(2);

        when(outboundOrderMapper.selectById(304L)).thenReturn(order);
        when(outboundOrderItemMapper.selectEntityListByOutboundOrderId(304L)).thenReturn(List.of(item));
        when(outboundOrderMapper.updateStatus(304L, OrderStatusConstant.OUTBOUND_COMPLETED, OrderStatusConstant.OUTBOUND_DRAFT)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.confirmOutboundOrder(304L));

        assertEquals("出库单状态已变化，请刷新后重试", exception.getMessage());
        verify(stockFlowService).decreaseByOutbound(eq(304L), eq("OUT304"), any(List.class), eq("tester"), eq(StockChangeTypeConstant.MANUAL_OUTBOUND), eq("draft"));
        verify(operationLogService, never()).recordSuccess(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmOutboundOrder_shouldThrow_whenStockInsufficient() {
        CurrentUserContext.setRole("ADMIN");
        CurrentUserContext.setUsername("tester");

        OutboundOrder order = buildOutboundOrder(600L, "OUT600", OrderStatusConstant.OUTBOUND_DRAFT, "draft");
        OutboundOrderItem item = new OutboundOrderItem();
        item.setOutboundOrderId(600L);
        item.setProductId(12L);
        item.setQuantity(999);

        when(outboundOrderMapper.selectById(600L)).thenReturn(order);
        when(outboundOrderItemMapper.selectEntityListByOutboundOrderId(600L)).thenReturn(List.of(item));
        doThrow(new BusinessException("库存不足，无法出库")).when(stockFlowService).decreaseByOutbound(any(), any(), any(), any(), any(), any());

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.confirmOutboundOrder(600L));

        assertEquals("库存不足，无法出库", exception.getMessage());
        verify(outboundOrderMapper, never()).updateStatus(any(), any(), any());
        verify(operationLogService, never()).recordSuccess(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void voidOutboundOrder_shouldRollbackStock_whenCompleted() {
        CurrentUserContext.setRole("ADMIN");
        CurrentUserContext.setUsername("tester");

        OutboundOrder order = buildOutboundOrder(400L, "OUT400", OrderStatusConstant.OUTBOUND_COMPLETED, "old remark");
        OutboundOrderItem item = new OutboundOrderItem();
        item.setOutboundOrderId(400L);
        item.setProductId(4L);
        item.setQuantity(4);

        when(outboundOrderMapper.selectById(400L)).thenReturn(order);
        when(outboundOrderItemMapper.selectEntityListByOutboundOrderId(400L)).thenReturn(List.of(item));
        when(outboundOrderMapper.updateStatus(400L, OrderStatusConstant.OUTBOUND_VOID, OrderStatusConstant.OUTBOUND_COMPLETED)).thenReturn(1);

        String result = outboundOrderService.voidOutboundOrder(400L, "客户退货");

        assertEquals("作废出库单成功，库存已回补", result);
        verify(stockFlowService).rollbackOutboundOnVoid(eq(400L), eq("OUT400"), any(List.class), eq("tester"), eq("原单备注：old remark；作废原因：客户退货"));
        verify(outboundOrderMapper).updateStatus(400L, OrderStatusConstant.OUTBOUND_VOID, OrderStatusConstant.OUTBOUND_COMPLETED);
    }

    @Test
    void voidOutboundOrder_shouldThrow_whenOrderDoesNotExist() {
        CurrentUserContext.setRole("ADMIN");
        when(outboundOrderMapper.selectById(400L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.voidOutboundOrder(400L, "客户退货"));

        assertEquals("出库单不存在", exception.getMessage());
    }

    @Test
    void voidOutboundOrder_shouldThrow_whenAlreadyVoid() {
        CurrentUserContext.setRole("ADMIN");
        when(outboundOrderMapper.selectById(401L)).thenReturn(buildOutboundOrder(401L, "OUT401", OrderStatusConstant.OUTBOUND_VOID, "void"));

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.voidOutboundOrder(401L, "客户退货"));

        assertEquals("出库单已作废，请勿重复操作", exception.getMessage());
    }

    @Test
    void voidOutboundOrder_shouldVoidDraft_whenDraft() {
        CurrentUserContext.setRole("ADMIN");
        CurrentUserContext.setUsername("tester");

        when(outboundOrderMapper.selectById(402L)).thenReturn(buildOutboundOrder(402L, "OUT402", OrderStatusConstant.OUTBOUND_DRAFT, "draft"));
        when(outboundOrderMapper.updateStatus(402L, OrderStatusConstant.OUTBOUND_VOID, OrderStatusConstant.OUTBOUND_DRAFT)).thenReturn(1);

        String result = outboundOrderService.voidOutboundOrder(402L, "录入错误");

        assertEquals("作废出库单成功", result);
        verify(stockFlowService, never()).rollbackOutboundOnVoid(any(), any(), any(), any(), any());
        verify(operationLogService).recordSuccess(any(), any(), any(), eq(402L), eq("OUT402"), eq("tester"), eq("作废出库单（草稿）"));
    }

    @Test
    void voidOutboundOrder_shouldThrow_whenCompletedItemsEmpty() {
        CurrentUserContext.setRole("ADMIN");
        when(outboundOrderMapper.selectById(403L)).thenReturn(buildOutboundOrder(403L, "OUT403", OrderStatusConstant.OUTBOUND_COMPLETED, "done"));
        when(outboundOrderItemMapper.selectEntityListByOutboundOrderId(403L)).thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.voidOutboundOrder(403L, "录入错误"));

        assertEquals("出库单明细不能为空", exception.getMessage());
    }

    @Test
    void voidOutboundOrder_shouldThrow_whenCompletedUpdateStatusFails() {
        CurrentUserContext.setRole("ADMIN");
        CurrentUserContext.setUsername("tester");

        OutboundOrder order = buildOutboundOrder(404L, "OUT404", OrderStatusConstant.OUTBOUND_COMPLETED, "done");
        OutboundOrderItem item = new OutboundOrderItem();
        item.setOutboundOrderId(404L);
        item.setProductId(10L);
        item.setQuantity(2);

        when(outboundOrderMapper.selectById(404L)).thenReturn(order);
        when(outboundOrderItemMapper.selectEntityListByOutboundOrderId(404L)).thenReturn(List.of(item));
        when(outboundOrderMapper.updateStatus(404L, OrderStatusConstant.OUTBOUND_VOID, OrderStatusConstant.OUTBOUND_COMPLETED)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.voidOutboundOrder(404L, "录入错误"));

        assertEquals("出库单状态已变化，请刷新后重试", exception.getMessage());
        verify(stockFlowService).rollbackOutboundOnVoid(eq(404L), eq("OUT404"), any(List.class), eq("tester"), eq("原单备注：done；作废原因：录入错误"));
        verify(operationLogService, never()).recordSuccess(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getOutboundOrderPage_shouldNormalizeParamsAndReturnPage() {
        OutboundOrderVO orderVO = new OutboundOrderVO();
        orderVO.setId(700L);
        orderVO.setOrderNo("OUT700");

        when(outboundOrderMapper.count("OUT", OrderStatusConstant.OUTBOUND_DRAFT)).thenReturn(1L);
        when(outboundOrderMapper.selectPage("OUT", OrderStatusConstant.OUTBOUND_DRAFT, 0, 200)).thenReturn(List.of(orderVO));

        PageResult<OutboundOrderVO> result = outboundOrderService.getOutboundOrderPage("OUT", OrderStatusConstant.OUTBOUND_DRAFT, 0, 500);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getPageNum());
        assertEquals(200, result.getPageSize());
        assertEquals(1, result.getList().size());
        assertEquals("OUT700", result.getList().get(0).getOrderNo());
    }

    @Test
    void getOutboundOrderPage_shouldThrow_whenOrderStatusInvalid() {
        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.getOutboundOrderPage("OUT", 99, 1, 10));

        assertEquals("状态参数无效", exception.getMessage());
        verify(outboundOrderMapper, never()).count(any(), any());
    }

    @Test
    void getOutboundOrderDetail_shouldReturnItems_whenExists() {
        OutboundOrderDetailVO detailVO = buildOutboundOrderDetailVO();
        OutboundOrderItemVO itemVO = new OutboundOrderItemVO();
        itemVO.setProductCode("P-10");
        itemVO.setProductName("商品A");

        when(outboundOrderMapper.selectDetailById(900L)).thenReturn(detailVO);
        when(outboundOrderItemMapper.selectByOutboundOrderId(900L)).thenReturn(List.of(itemVO));

        OutboundOrderDetailVO result = outboundOrderService.getOutboundOrderDetail(900L);

        assertEquals("OUT900", result.getOrderNo());
        assertEquals(1, result.getItemList().size());
        assertEquals("P-10", result.getItemList().get(0).getProductCode());
    }

    @Test
    void getOutboundOrderDetail_shouldThrow_whenNotFound() {
        when(outboundOrderMapper.selectDetailById(900L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.getOutboundOrderDetail(900L));

        assertEquals("出库单不存在", exception.getMessage());
    }

    @Test
    void exportOutboundOrderExcel_shouldExportWorkbook_whenDetailExists() throws Exception {
        OutboundOrderDetailVO detailVO = buildOutboundOrderDetailVO();
        OutboundOrderItemVO itemVO = new OutboundOrderItemVO();
        itemVO.setProductCode("P-10");
        itemVO.setProductName("商品A");
        itemVO.setSpecification("500g");
        itemVO.setUnit("袋");
        itemVO.setQuantity(2);
        itemVO.setUnitPrice(new BigDecimal("12.50"));
        itemVO.setAmount(new BigDecimal("25.00"));
        itemVO.setRemark("第一行");

        when(outboundOrderMapper.selectDetailById(900L)).thenReturn(detailVO);
        when(outboundOrderItemMapper.selectByOutboundOrderId(900L)).thenReturn(List.of(itemVO));

        byte[] bytes = outboundOrderService.exportOutboundOrderExcel(900L);

        assertTrue(bytes.length > 0);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals("出库单", workbook.getSheetAt(0).getSheetName());
            assertEquals("出库单号", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("OUT900", workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue());
            assertEquals("商品编码", workbook.getSheetAt(0).getRow(4).getCell(1).getStringCellValue());
            assertEquals("P-10", workbook.getSheetAt(0).getRow(5).getCell(1).getStringCellValue());
        }
    }

    @Test
    void exportOutboundOrderPdf_shouldReturnPdfBytes_whenDetailExists() {
        OutboundOrderDetailVO detailVO = buildOutboundOrderDetailVO();
        OutboundOrderItemVO itemVO = new OutboundOrderItemVO();
        itemVO.setProductCode("P-10");
        itemVO.setProductName("商品A");
        itemVO.setSpecification("500g");
        itemVO.setUnit("袋");
        itemVO.setQuantity(2);
        itemVO.setUnitPrice(new BigDecimal("12.50"));
        itemVO.setAmount(new BigDecimal("25.00"));
        itemVO.setRemark("第一行");

        when(outboundOrderMapper.selectDetailById(900L)).thenReturn(detailVO);
        when(outboundOrderItemMapper.selectByOutboundOrderId(900L)).thenReturn(List.of(itemVO));

        byte[] bytes = outboundOrderService.exportOutboundOrderPdf(900L);

        assertNotNull(bytes);
        assertTrue(bytes.length > 4);
        assertEquals("%PDF", new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
    }

    private OutboundOrderAddDTO buildSingleItemOrderDto() {
        OutboundOrderAddDTO dto = new OutboundOrderAddDTO();
        dto.setCustomerId(66L);
        dto.setRemark("首单");
        dto.setItemList(List.of(createItemDto(10L, 2, "12.50", "第一行")));
        return dto;
    }

    private OutboundOrderItemAddDTO createItemDto(Long productId, int quantity, String unitPrice, String remark) {
        OutboundOrderItemAddDTO item = new OutboundOrderItemAddDTO();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setRemark(remark);
        return item;
    }

    private Customer createCustomer(Long id, String customerName, Integer status) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setCustomerName(customerName);
        customer.setStatus(status);
        return customer;
    }

    private Product createProduct(Long id, String productName, String specification, String unit) {
        Product product = new Product();
        product.setId(id);
        product.setProductName(productName);
        product.setSpecification(specification);
        product.setUnit(unit);
        return product;
    }

    private StockVO createStock(Long productId, Integer quantity) {
        StockVO stockVO = new StockVO();
        stockVO.setProductId(productId);
        stockVO.setQuantity(quantity);
        return stockVO;
    }

    private OutboundOrder buildOutboundOrder(Long id, String orderNo, Integer orderStatus, String remark) {
        OutboundOrder order = new OutboundOrder();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setOrderStatus(orderStatus);
        order.setRemark(remark);
        return order;
    }

    private OutboundOrderDetailVO buildOutboundOrderDetailVO() {
        OutboundOrderDetailVO detailVO = new OutboundOrderDetailVO();
        detailVO.setId(900L);
        detailVO.setOrderNo("OUT900");
        detailVO.setCustomerName("客户A");
        detailVO.setTotalAmount(new BigDecimal("25.00"));
        detailVO.setOrderStatus(OrderStatusConstant.OUTBOUND_COMPLETED);
        detailVO.setRemark("remark");
        detailVO.setCreatedTime(LocalDateTime.of(2026, 4, 15, 10, 30, 0));
        return detailVO;
    }
}
