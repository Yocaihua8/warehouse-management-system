package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.CurrentUserContext;
import com.yocaihua.wms.common.OrderStatusConstant;
import com.yocaihua.wms.common.StockChangeTypeConstant;
import com.yocaihua.wms.entity.OutboundOrder;
import com.yocaihua.wms.entity.OutboundOrderItem;
import com.yocaihua.wms.mapper.CustomerMapper;
import com.yocaihua.wms.mapper.OutboundOrderItemMapper;
import com.yocaihua.wms.mapper.OutboundOrderMapper;
import com.yocaihua.wms.mapper.ProductMapper;
import com.yocaihua.wms.mapper.StockMapper;
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
    void confirmOutboundOrder_shouldDecreaseStockAndComplete_whenDraft() {
        CurrentUserContext.setRole("ADMIN");
        CurrentUserContext.setUsername("tester");

        OutboundOrder order = new OutboundOrder();
        order.setId(300L);
        order.setOrderNo("OUT300");
        order.setOrderStatus(OrderStatusConstant.OUTBOUND_DRAFT);
        order.setRemark("manual outbound");

        OutboundOrderItem item = new OutboundOrderItem();
        item.setOutboundOrderId(300L);
        item.setProductId(3L);
        item.setQuantity(6);

        when(outboundOrderMapper.selectById(300L)).thenReturn(order);
        when(outboundOrderItemMapper.selectEntityListByOutboundOrderId(300L)).thenReturn(List.of(item));
        when(outboundOrderMapper.updateStatus(300L, OrderStatusConstant.OUTBOUND_COMPLETED, OrderStatusConstant.OUTBOUND_DRAFT)).thenReturn(1);

        String result = outboundOrderService.confirmOutboundOrder(300L);

        assertEquals("确认出库成功", result);
        verify(stockFlowService).decreaseByOutbound(
                eq(300L),
                eq("OUT300"),
                any(List.class),
                eq("tester"),
                eq(StockChangeTypeConstant.MANUAL_OUTBOUND),
                eq("manual outbound")
        );
        verify(outboundOrderMapper).updateStatus(300L, OrderStatusConstant.OUTBOUND_COMPLETED, OrderStatusConstant.OUTBOUND_DRAFT);
    }

    @Test
    void confirmOutboundOrder_shouldThrow_whenAlreadyCompleted() {
        CurrentUserContext.setRole("ADMIN");
        CurrentUserContext.setUsername("tester");

        OutboundOrder order = new OutboundOrder();
        order.setId(301L);
        order.setOrderStatus(OrderStatusConstant.OUTBOUND_COMPLETED);
        when(outboundOrderMapper.selectById(301L)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class, () -> outboundOrderService.confirmOutboundOrder(301L));

        assertEquals("出库单已确认出库，请勿重复操作", exception.getMessage());
        verify(stockFlowService, never()).decreaseByOutbound(any(), any(), any(), any(), any(), any());
    }

    @Test
    void voidOutboundOrder_shouldRollbackStock_whenCompleted() {
        CurrentUserContext.setRole("ADMIN");
        CurrentUserContext.setUsername("tester");

        OutboundOrder order = new OutboundOrder();
        order.setId(400L);
        order.setOrderNo("OUT400");
        order.setOrderStatus(OrderStatusConstant.OUTBOUND_COMPLETED);
        order.setRemark("old remark");

        OutboundOrderItem item = new OutboundOrderItem();
        item.setOutboundOrderId(400L);
        item.setProductId(4L);
        item.setQuantity(4);

        when(outboundOrderMapper.selectById(400L)).thenReturn(order);
        when(outboundOrderItemMapper.selectEntityListByOutboundOrderId(400L)).thenReturn(List.of(item));
        when(outboundOrderMapper.updateStatus(400L, OrderStatusConstant.OUTBOUND_VOID, OrderStatusConstant.OUTBOUND_COMPLETED)).thenReturn(1);

        String result = outboundOrderService.voidOutboundOrder(400L, "客户退货");

        assertEquals("作废出库单成功，库存已回补", result);
        verify(stockFlowService).rollbackOutboundOnVoid(
                eq(400L),
                eq("OUT400"),
                any(List.class),
                eq("tester"),
                eq("原单备注：old remark；作废原因：客户退货")
        );
        verify(outboundOrderMapper).updateStatus(400L, OrderStatusConstant.OUTBOUND_VOID, OrderStatusConstant.OUTBOUND_COMPLETED);
    }
}
