package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.StockChangeTypeConstant;
import com.yocaihua.wms.entity.InboundOrderItem;
import com.yocaihua.wms.entity.OutboundOrderItem;
import com.yocaihua.wms.entity.StockAdjustLog;
import com.yocaihua.wms.mapper.StockAdjustLogMapper;
import com.yocaihua.wms.mapper.StockMapper;
import com.yocaihua.wms.vo.StockVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class StockFlowServiceImplTest {

    @Mock
    private StockMapper stockMapper;

    @Mock
    private StockAdjustLogMapper stockAdjustLogMapper;

    @InjectMocks
    private StockFlowServiceImpl stockFlowService;

    @Test
    void increaseByInbound_shouldReturnImmediately_whenItemListEmpty() {
        stockFlowService.increaseByInbound(1L, "IN001", List.of(), "tester", StockChangeTypeConstant.MANUAL_INBOUND, "备注");

        verify(stockMapper, never()).selectByProductId(any());
        verify(stockAdjustLogMapper, never()).insert(any());
    }

    @Test
    void increaseByInbound_shouldThrow_whenStockRecordMissing() {
        InboundOrderItem item = inboundItem(10L, "商品A", 5);
        when(stockMapper.selectByProductId(10L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> stockFlowService.increaseByInbound(1L, "IN001", List.of(item), "tester", StockChangeTypeConstant.MANUAL_INBOUND, "备注"));

        assertEquals("库存记录不存在，productId=10", exception.getMessage());
    }

    @Test
    void increaseByInbound_shouldWriteLog_whenManualInboundSucceeds() {
        InboundOrderItem item = inboundItem(10L, "商品快照", 5);
        StockVO stock = stock(10L, "库存商品", 8);

        when(stockMapper.selectByProductId(10L)).thenReturn(stock);
        when(stockMapper.increaseStock(10L, 5)).thenReturn(1);

        stockFlowService.increaseByInbound(1L, "IN001", List.of(item), "tester", StockChangeTypeConstant.MANUAL_INBOUND, "手工入库");

        ArgumentCaptor<StockAdjustLog> captor = ArgumentCaptor.forClass(StockAdjustLog.class);
        verify(stockAdjustLogMapper).insert(captor.capture());

        StockAdjustLog log = captor.getValue();
        assertEquals(10L, log.getProductId());
        assertEquals("商品快照", log.getProductNameSnapshot());
        assertEquals(8, log.getBeforeQuantity());
        assertEquals(13, log.getAfterQuantity());
        assertEquals(5, log.getChangeQuantity());
        assertEquals(StockChangeTypeConstant.MANUAL_INBOUND, log.getChangeType());
        assertEquals("手工入库增加库存", log.getReason());
        assertEquals("手工入库", log.getRemark());
    }

    @Test
    void increaseByInbound_shouldUseStockNameAndAiReason_whenSnapshotBlank() {
        InboundOrderItem item = inboundItem(10L, "   ", 5);
        StockVO stock = stock(10L, "库存商品", 8);

        when(stockMapper.selectByProductId(10L)).thenReturn(stock);
        when(stockMapper.increaseStock(10L, 5)).thenReturn(1);

        stockFlowService.increaseByInbound(1L, "IN001", List.of(item), "tester", StockChangeTypeConstant.AI_CONFIRM_INBOUND, "AI备注");

        ArgumentCaptor<StockAdjustLog> captor = ArgumentCaptor.forClass(StockAdjustLog.class);
        verify(stockAdjustLogMapper).insert(captor.capture());

        StockAdjustLog log = captor.getValue();
        assertEquals("库存商品", log.getProductNameSnapshot());
        assertEquals("AI确认入库增加库存", log.getReason());
    }

    @Test
    void decreaseByOutbound_shouldThrow_whenInsufficientBeforeDecrease() {
        OutboundOrderItem item = outboundItem(20L, "商品A", 9);
        when(stockMapper.selectByProductId(20L)).thenReturn(stock(20L, "库存商品", 3));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> stockFlowService.decreaseByOutbound(2L, "OUT001", List.of(item), "tester", StockChangeTypeConstant.MANUAL_OUTBOUND, "备注"));

        assertEquals("出库单[OUT001]确认失败：商品[商品A]库存不足，需出库9，当前可用3。请刷新草稿后重试", exception.getMessage());
        verify(stockAdjustLogMapper, never()).insert(any());
    }

    @Test
    void decreaseByOutbound_shouldThrowWithLatestQuantity_whenDecreaseFails() {
        OutboundOrderItem item = outboundItem(20L, "商品A", 9);
        when(stockMapper.selectByProductId(20L)).thenReturn(stock(20L, "库存商品", 10), stock(20L, "库存商品", 4));
        when(stockMapper.decreaseStock(20L, 9)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> stockFlowService.decreaseByOutbound(2L, "OUT001", List.of(item), "tester", StockChangeTypeConstant.MANUAL_OUTBOUND, "备注"));

        assertEquals("出库单[OUT001]确认失败：商品[商品A]库存不足，需出库9，当前可用4。请刷新草稿后重试", exception.getMessage());
    }

    @Test
    void decreaseByOutbound_shouldWriteLog_whenAiOutboundSucceeds() {
        OutboundOrderItem item = outboundItem(20L, "   ", 6);
        when(stockMapper.selectByProductId(20L)).thenReturn(stock(20L, "库存商品", 10));
        when(stockMapper.decreaseStock(20L, 6)).thenReturn(1);

        stockFlowService.decreaseByOutbound(2L, "OUT001", List.of(item), "tester", StockChangeTypeConstant.AI_CONFIRM_OUTBOUND, "AI出库");

        ArgumentCaptor<StockAdjustLog> captor = ArgumentCaptor.forClass(StockAdjustLog.class);
        verify(stockAdjustLogMapper).insert(captor.capture());

        StockAdjustLog log = captor.getValue();
        assertEquals("库存商品", log.getProductNameSnapshot());
        assertEquals(10, log.getBeforeQuantity());
        assertEquals(4, log.getAfterQuantity());
        assertEquals(-6, log.getChangeQuantity());
        assertEquals("AI确认出库扣减库存", log.getReason());
    }

    @Test
    void rollbackInboundOnVoid_shouldThrow_whenCurrentStockInsufficient() {
        InboundOrderItem item = inboundItem(30L, "商品A", 7);
        when(stockMapper.selectByProductId(30L)).thenReturn(stock(30L, "库存商品", 2));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> stockFlowService.rollbackInboundOnVoid(3L, "IN003", List.of(item), "tester", "作废"));

        assertEquals("作废入库单失败，当前库存不足以回退商品，productId=30", exception.getMessage());
    }

    @Test
    void rollbackInboundOnVoid_shouldWriteVoidInboundLog_whenSucceeded() {
        InboundOrderItem item = inboundItem(30L, "商品A", 7);
        when(stockMapper.selectByProductId(30L)).thenReturn(stock(30L, "库存商品", 10));
        when(stockMapper.decreaseStock(30L, 7)).thenReturn(1);

        stockFlowService.rollbackInboundOnVoid(3L, "IN003", List.of(item), "tester", "作废备注");

        ArgumentCaptor<StockAdjustLog> captor = ArgumentCaptor.forClass(StockAdjustLog.class);
        verify(stockAdjustLogMapper).insert(captor.capture());

        StockAdjustLog log = captor.getValue();
        assertEquals(StockChangeTypeConstant.VOID_INBOUND, log.getChangeType());
        assertEquals("作废已入库单据，回退库存", log.getReason());
        assertEquals(-7, log.getChangeQuantity());
    }

    @Test
    void rollbackOutboundOnVoid_shouldThrow_whenIncreaseFails() {
        OutboundOrderItem item = outboundItem(40L, "商品A", 3);
        when(stockMapper.selectByProductId(40L)).thenReturn(stock(40L, "库存商品", 5));
        when(stockMapper.increaseStock(40L, 3)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> stockFlowService.rollbackOutboundOnVoid(4L, "OUT004", List.of(item), "tester", "作废备注"));

        assertEquals("作废出库单回补库存失败，productId=40", exception.getMessage());
    }

    @Test
    void rollbackOutboundOnVoid_shouldWriteVoidOutboundLog_whenSucceeded() {
        OutboundOrderItem item = outboundItem(40L, "商品A", 3);
        when(stockMapper.selectByProductId(40L)).thenReturn(stock(40L, "库存商品", 5));
        when(stockMapper.increaseStock(40L, 3)).thenReturn(1);

        stockFlowService.rollbackOutboundOnVoid(4L, "OUT004", List.of(item), "tester", "作废备注");

        ArgumentCaptor<StockAdjustLog> captor = ArgumentCaptor.forClass(StockAdjustLog.class);
        verify(stockAdjustLogMapper).insert(captor.capture());

        StockAdjustLog log = captor.getValue();
        assertEquals(StockChangeTypeConstant.VOID_OUTBOUND, log.getChangeType());
        assertEquals("作废已出库单据，回补库存", log.getReason());
        assertEquals(3, log.getChangeQuantity());
        assertEquals(8, log.getAfterQuantity());
    }

    @Test
    void recordManualAdjust_shouldWriteManualAdjustLog() {
        stockFlowService.recordManualAdjust(50L, "商品A", 8, 12, "tester", "盘点修正", "库存管理页手工修改");

        ArgumentCaptor<StockAdjustLog> captor = ArgumentCaptor.forClass(StockAdjustLog.class);
        verify(stockAdjustLogMapper).insert(captor.capture());

        StockAdjustLog log = captor.getValue();
        assertEquals(50L, log.getProductId());
        assertEquals("商品A", log.getProductNameSnapshot());
        assertEquals(8, log.getBeforeQuantity());
        assertEquals(12, log.getAfterQuantity());
        assertEquals(4, log.getChangeQuantity());
        assertEquals(StockChangeTypeConstant.MANUAL_ADJUST, log.getChangeType());
        assertEquals("盘点修正", log.getReason());
    }

    private InboundOrderItem inboundItem(Long productId, String productNameSnapshot, Integer quantity) {
        InboundOrderItem item = new InboundOrderItem();
        item.setProductId(productId);
        item.setProductNameSnapshot(productNameSnapshot);
        item.setQuantity(quantity);
        return item;
    }

    private OutboundOrderItem outboundItem(Long productId, String productNameSnapshot, Integer quantity) {
        OutboundOrderItem item = new OutboundOrderItem();
        item.setProductId(productId);
        item.setProductNameSnapshot(productNameSnapshot);
        item.setQuantity(quantity);
        return item;
    }

    private StockVO stock(Long productId, String productName, Integer quantity) {
        StockVO stock = new StockVO();
        stock.setProductId(productId);
        stock.setProductName(productName);
        stock.setQuantity(quantity);
        return stock;
    }
}
