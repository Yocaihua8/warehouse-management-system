package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.CurrentUserContext;
import com.yocaihua.wms.common.OrderStatusConstant;
import com.yocaihua.wms.common.StockChangeTypeConstant;
import com.yocaihua.wms.entity.InboundOrder;
import com.yocaihua.wms.entity.InboundOrderItem;
import com.yocaihua.wms.mapper.InboundOrderItemMapper;
import com.yocaihua.wms.mapper.InboundOrderMapper;
import com.yocaihua.wms.mapper.ProductMapper;
import com.yocaihua.wms.mapper.StockMapper;
import com.yocaihua.wms.mapper.SupplierMapper;
import com.yocaihua.wms.service.OperationLogService;
import com.yocaihua.wms.service.StockFlowService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboundOrderServiceImplTest {

    @Mock
    private InboundOrderMapper inboundOrderMapper;
    @Mock
    private InboundOrderItemMapper inboundOrderItemMapper;
    @Mock
    private StockMapper stockMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private SupplierMapper supplierMapper;
    @Mock
    private StockFlowService stockFlowService;
    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private InboundOrderServiceImpl inboundOrderService;

    @BeforeEach
    void setUp() {
        CurrentUserContext.clear();
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void confirmInboundOrder_shouldIncreaseStockAndComplete_whenDraft() {
        CurrentUserContext.setRole("ADMIN");
        CurrentUserContext.setUsername("tester");

        InboundOrder order = new InboundOrder();
        order.setId(100L);
        order.setOrderNo("IN100");
        order.setOrderStatus(OrderStatusConstant.INBOUND_DRAFT);
        order.setRemark("manual inbound");

        InboundOrderItem item = new InboundOrderItem();
        item.setInboundOrderId(100L);
        item.setProductId(1L);
        item.setQuantity(10);

        when(inboundOrderMapper.selectById(100L)).thenReturn(order);
        when(inboundOrderItemMapper.selectEntityListByInboundOrderId(100L)).thenReturn(List.of(item));
        when(inboundOrderMapper.updateStatus(100L, OrderStatusConstant.INBOUND_COMPLETED, OrderStatusConstant.INBOUND_DRAFT)).thenReturn(1);

        String result = inboundOrderService.confirmInboundOrder(100L);

        assertEquals("确认入库成功", result);
        verify(stockFlowService).increaseByInbound(
                eq(100L),
                eq("IN100"),
                any(List.class),
                eq("tester"),
                eq(StockChangeTypeConstant.MANUAL_INBOUND),
                eq("manual inbound")
        );
        verify(inboundOrderMapper).updateStatus(100L, OrderStatusConstant.INBOUND_COMPLETED, OrderStatusConstant.INBOUND_DRAFT);
    }

    @Test
    void confirmInboundOrder_shouldThrow_whenNotAdmin() {
        CurrentUserContext.setRole("OPERATOR");
        CurrentUserContext.setUsername("operator");

        BusinessException exception = assertThrows(BusinessException.class, () -> inboundOrderService.confirmInboundOrder(100L));

        assertEquals("仅管理员可执行：确认入库", exception.getMessage());
        verify(inboundOrderMapper, never()).selectById(any());
        verify(stockFlowService, never()).increaseByInbound(any(), any(), any(), any(), any(), any());
    }

    @Test
    void voidInboundOrder_shouldRollbackStock_whenCompleted() {
        CurrentUserContext.setRole("ADMIN");
        CurrentUserContext.setUsername("tester");

        InboundOrder order = new InboundOrder();
        order.setId(200L);
        order.setOrderNo("IN200");
        order.setOrderStatus(OrderStatusConstant.INBOUND_COMPLETED);
        order.setRemark("old remark");

        InboundOrderItem item = new InboundOrderItem();
        item.setInboundOrderId(200L);
        item.setProductId(2L);
        item.setQuantity(5);

        when(inboundOrderMapper.selectById(200L)).thenReturn(order);
        when(inboundOrderItemMapper.selectEntityListByInboundOrderId(200L)).thenReturn(List.of(item));
        when(inboundOrderMapper.updateStatus(200L, OrderStatusConstant.INBOUND_VOID, OrderStatusConstant.INBOUND_COMPLETED)).thenReturn(1);

        String result = inboundOrderService.voidInboundOrder(200L, "录入错误");

        assertEquals("作废入库单成功，库存已回退", result);
        verify(stockFlowService).rollbackInboundOnVoid(
                eq(200L),
                eq("IN200"),
                any(List.class),
                eq("tester"),
                eq("原单备注：old remark；作废原因：录入错误")
        );
        verify(inboundOrderMapper).updateStatus(200L, OrderStatusConstant.INBOUND_VOID, OrderStatusConstant.INBOUND_COMPLETED);
    }
}
