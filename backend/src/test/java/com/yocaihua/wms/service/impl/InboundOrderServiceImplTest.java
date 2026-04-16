package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.CurrentUserContext;
import com.yocaihua.wms.common.OrderStatusConstant;
import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.StockChangeTypeConstant;
import com.yocaihua.wms.dto.InboundOrderAddDTO;
import com.yocaihua.wms.dto.InboundOrderItemAddDTO;
import com.yocaihua.wms.entity.InboundOrder;
import com.yocaihua.wms.entity.InboundOrderItem;
import com.yocaihua.wms.entity.Product;
import com.yocaihua.wms.entity.Supplier;
import com.yocaihua.wms.mapper.InboundOrderItemMapper;
import com.yocaihua.wms.mapper.InboundOrderMapper;
import com.yocaihua.wms.mapper.ProductMapper;
import com.yocaihua.wms.mapper.StockMapper;
import com.yocaihua.wms.mapper.SupplierMapper;
import com.yocaihua.wms.service.OperationLogService;
import com.yocaihua.wms.service.StockFlowService;
import com.yocaihua.wms.vo.InboundDetailVO;
import com.yocaihua.wms.vo.InboundItemVO;
import com.yocaihua.wms.vo.InboundOrderDetailVO;
import com.yocaihua.wms.vo.InboundOrderItemVO;
import com.yocaihua.wms.vo.InboundOrderVO;
import com.yocaihua.wms.vo.OrderCreatedVO;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
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
    void saveInboundOrder_shouldPersistOrderAndItems_whenValid() {
        InboundOrderAddDTO dto = new InboundOrderAddDTO();
        dto.setSupplierName("  供应商A  ");
        dto.setRemark("首单");

        InboundOrderItemAddDTO firstItem = new InboundOrderItemAddDTO();
        firstItem.setProductId(10L);
        firstItem.setQuantity(2);
        firstItem.setUnitPrice(new BigDecimal("12.50"));
        firstItem.setRemark("第一行");

        InboundOrderItemAddDTO secondItem = new InboundOrderItemAddDTO();
        secondItem.setProductId(11L);
        secondItem.setQuantity(1);
        secondItem.setUnitPrice(new BigDecimal("10.50"));
        secondItem.setRemark("第二行");

        dto.setItemList(List.of(firstItem, secondItem));

        Supplier supplier = new Supplier();
        supplier.setId(88L);
        supplier.setStatus(1);

        Product productA = new Product();
        productA.setId(10L);
        productA.setProductName("商品A");
        productA.setSpecification("500g");
        productA.setUnit("袋");

        Product productB = new Product();
        productB.setId(11L);
        productB.setProductName("商品B");
        productB.setSpecification("1kg");
        productB.setUnit("箱");

        StockVO stockA = new StockVO();
        stockA.setProductId(10L);
        stockA.setQuantity(100);

        StockVO stockB = new StockVO();
        stockB.setProductId(11L);
        stockB.setQuantity(60);

        when(supplierMapper.selectByName("供应商A")).thenReturn(supplier);
        when(productMapper.selectById(10L)).thenReturn(productA);
        when(productMapper.selectById(11L)).thenReturn(productB);
        when(stockMapper.selectByProductId(10L)).thenReturn(stockA);
        when(stockMapper.selectByProductId(11L)).thenReturn(stockB);
        when(inboundOrderMapper.insert(any(InboundOrder.class))).thenAnswer(invocation -> {
            InboundOrder order = invocation.getArgument(0);
            order.setId(300L);
            return 1;
        });
        when(inboundOrderItemMapper.insert(any(InboundOrderItem.class))).thenReturn(1);

        OrderCreatedVO result = inboundOrderService.saveInboundOrder(dto);

        assertEquals(300L, result.getId());
        assertTrue(result.getOrderNo().startsWith("IN"));
        verify(inboundOrderMapper).insert(argThat(order ->
                order.getSupplierId().equals(88L)
                        && "供应商A".equals(order.getSupplierName())
                        && "首单".equals(order.getRemark())
                        && OrderStatusConstant.INBOUND_DRAFT.equals(order.getOrderStatus())
                        && order.getOrderNo() != null
                        && order.getOrderNo().startsWith("IN")
                        && order.getTotalAmount().compareTo(new BigDecimal("35.50")) == 0
        ));

        ArgumentCaptor<InboundOrderItem> itemCaptor = ArgumentCaptor.forClass(InboundOrderItem.class);
        verify(inboundOrderItemMapper, times(2)).insert(itemCaptor.capture());
        List<InboundOrderItem> savedItems = itemCaptor.getAllValues();

        InboundOrderItem savedFirstItem = savedItems.get(0);
        assertEquals(300L, savedFirstItem.getInboundOrderId());
        assertEquals(10L, savedFirstItem.getProductId());
        assertEquals("商品A", savedFirstItem.getProductNameSnapshot());
        assertEquals("500g", savedFirstItem.getSpecificationSnapshot());
        assertEquals("袋", savedFirstItem.getUnitSnapshot());
        assertEquals(2, savedFirstItem.getQuantity());
        assertEquals(0, savedFirstItem.getUnitPrice().compareTo(new BigDecimal("12.50")));
        assertEquals(0, savedFirstItem.getAmount().compareTo(new BigDecimal("25.00")));
        assertEquals("第一行", savedFirstItem.getRemark());

        InboundOrderItem savedSecondItem = savedItems.get(1);
        assertEquals(300L, savedSecondItem.getInboundOrderId());
        assertEquals(11L, savedSecondItem.getProductId());
        assertEquals("商品B", savedSecondItem.getProductNameSnapshot());
        assertEquals("1kg", savedSecondItem.getSpecificationSnapshot());
        assertEquals("箱", savedSecondItem.getUnitSnapshot());
        assertEquals(1, savedSecondItem.getQuantity());
        assertEquals(0, savedSecondItem.getUnitPrice().compareTo(new BigDecimal("10.50")));
        assertEquals(0, savedSecondItem.getAmount().compareTo(new BigDecimal("10.50")));
        assertEquals("第二行", savedSecondItem.getRemark());
    }

    @Test
    void saveInboundOrder_shouldThrow_whenDuplicateProductExists() {
        InboundOrderAddDTO dto = new InboundOrderAddDTO();
        dto.setSupplierName("供应商A");

        InboundOrderItemAddDTO firstItem = new InboundOrderItemAddDTO();
        firstItem.setProductId(10L);
        firstItem.setQuantity(2);
        firstItem.setUnitPrice(new BigDecimal("12.50"));

        InboundOrderItemAddDTO secondItem = new InboundOrderItemAddDTO();
        secondItem.setProductId(10L);
        secondItem.setQuantity(3);
        secondItem.setUnitPrice(new BigDecimal("9.90"));

        dto.setItemList(List.of(firstItem, secondItem));

        Product product = new Product();
        product.setId(10L);

        StockVO stock = new StockVO();
        stock.setProductId(10L);
        stock.setQuantity(100);

        when(supplierMapper.selectByName("供应商A")).thenReturn(null);
        when(productMapper.selectById(10L)).thenReturn(product);
        when(stockMapper.selectByProductId(10L)).thenReturn(stock);

        BusinessException exception = assertThrows(BusinessException.class, () -> inboundOrderService.saveInboundOrder(dto));

        assertEquals("同一商品不能重复出现在入库单中", exception.getMessage());
        verify(inboundOrderMapper, never()).insert(any(InboundOrder.class));
        verify(inboundOrderItemMapper, never()).insert(any(InboundOrderItem.class));
    }

    @Test
    void saveInboundOrder_shouldThrow_whenSupplierIsDisabled() {
        InboundOrderAddDTO dto = new InboundOrderAddDTO();
        dto.setSupplierName("供应商A");

        InboundOrderItemAddDTO item = new InboundOrderItemAddDTO();
        item.setProductId(10L);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("12.50"));
        dto.setItemList(List.of(item));

        Supplier supplier = new Supplier();
        supplier.setId(88L);
        supplier.setStatus(0);

        when(supplierMapper.selectByName("供应商A")).thenReturn(supplier);

        BusinessException exception = assertThrows(BusinessException.class, () -> inboundOrderService.saveInboundOrder(dto));

        assertEquals("供应商已停用，不能用于新入库单", exception.getMessage());
        verify(productMapper, never()).selectById(any());
        verify(stockMapper, never()).selectByProductId(any());
        verify(inboundOrderMapper, never()).insert(any(InboundOrder.class));
        verify(inboundOrderItemMapper, never()).insert(any(InboundOrderItem.class));
    }

    @Test
    void saveInboundOrder_shouldThrow_whenInsertOrderItemFails() {
        InboundOrderAddDTO dto = new InboundOrderAddDTO();
        dto.setSupplierName("供应商A");
        dto.setRemark("首单");

        InboundOrderItemAddDTO item = new InboundOrderItemAddDTO();
        item.setProductId(10L);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("12.50"));
        item.setRemark("第一行");
        dto.setItemList(List.of(item));

        Supplier supplier = new Supplier();
        supplier.setId(88L);
        supplier.setStatus(1);

        Product product = new Product();
        product.setId(10L);
        product.setProductName("商品A");
        product.setSpecification("500g");
        product.setUnit("袋");

        StockVO stock = new StockVO();
        stock.setProductId(10L);
        stock.setQuantity(100);

        when(supplierMapper.selectByName("供应商A")).thenReturn(supplier);
        when(productMapper.selectById(10L)).thenReturn(product);
        when(stockMapper.selectByProductId(10L)).thenReturn(stock);
        when(inboundOrderMapper.insert(any(InboundOrder.class))).thenAnswer(invocation -> {
            InboundOrder order = invocation.getArgument(0);
            order.setId(300L);
            return 1;
        });
        when(inboundOrderItemMapper.insert(any(InboundOrderItem.class))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> inboundOrderService.saveInboundOrder(dto));

        assertEquals("保存入库单明细失败", exception.getMessage());
        verify(inboundOrderMapper).insert(any(InboundOrder.class));
        verify(inboundOrderItemMapper).insert(any(InboundOrderItem.class));
    }

    @Test
    void saveInboundOrder_shouldThrow_whenProductDoesNotExist() {
        InboundOrderAddDTO dto = new InboundOrderAddDTO();
        dto.setSupplierName("供应商A");

        InboundOrderItemAddDTO item = new InboundOrderItemAddDTO();
        item.setProductId(10L);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("12.50"));
        dto.setItemList(List.of(item));

        when(supplierMapper.selectByName("供应商A")).thenReturn(null);
        when(productMapper.selectById(10L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> inboundOrderService.saveInboundOrder(dto));

        assertEquals("商品不存在，productId=10", exception.getMessage());
        verify(stockMapper, never()).selectByProductId(any());
        verify(inboundOrderMapper, never()).insert(any(InboundOrder.class));
        verify(inboundOrderItemMapper, never()).insert(any(InboundOrderItem.class));
    }

    @Test
    void saveInboundOrder_shouldThrow_whenStockRecordDoesNotExist() {
        InboundOrderAddDTO dto = new InboundOrderAddDTO();
        dto.setSupplierName("供应商A");

        InboundOrderItemAddDTO item = new InboundOrderItemAddDTO();
        item.setProductId(10L);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("12.50"));
        dto.setItemList(List.of(item));

        Product product = new Product();
        product.setId(10L);

        when(supplierMapper.selectByName("供应商A")).thenReturn(null);
        when(productMapper.selectById(10L)).thenReturn(product);
        when(stockMapper.selectByProductId(10L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> inboundOrderService.saveInboundOrder(dto));

        assertEquals("商品库存记录不存在，productId=10", exception.getMessage());
        verify(inboundOrderMapper, never()).insert(any(InboundOrder.class));
        verify(inboundOrderItemMapper, never()).insert(any(InboundOrderItem.class));
    }

    @Test
    void saveInboundOrder_shouldThrow_whenOrderInsertFails() {
        InboundOrderAddDTO dto = new InboundOrderAddDTO();
        dto.setSupplierName("供应商A");

        InboundOrderItemAddDTO item = new InboundOrderItemAddDTO();
        item.setProductId(10L);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("12.50"));
        dto.setItemList(List.of(item));

        Product product = new Product();
        product.setId(10L);
        product.setProductName("商品A");
        product.setSpecification("500g");
        product.setUnit("袋");

        StockVO stock = new StockVO();
        stock.setProductId(10L);
        stock.setQuantity(100);

        when(supplierMapper.selectByName("供应商A")).thenReturn(null);
        when(productMapper.selectById(10L)).thenReturn(product);
        when(stockMapper.selectByProductId(10L)).thenReturn(stock);
        when(inboundOrderMapper.insert(any(InboundOrder.class))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> inboundOrderService.saveInboundOrder(dto));

        assertEquals("保存入库单失败", exception.getMessage());
        verify(inboundOrderMapper).insert(any(InboundOrder.class));
        verify(inboundOrderItemMapper, never()).insert(any(InboundOrderItem.class));
    }

    @Test
    void saveInboundOrder_shouldThrow_whenItemListIsEmpty() {
        InboundOrderAddDTO dto = new InboundOrderAddDTO();
        dto.setSupplierName("供应商A");
        dto.setItemList(List.of());

        BusinessException exception = assertThrows(BusinessException.class, () -> inboundOrderService.saveInboundOrder(dto));

        assertEquals("入库单明细不能为空", exception.getMessage());
        verify(supplierMapper, never()).selectByName(any());
        verify(productMapper, never()).selectById(any());
        verify(stockMapper, never()).selectByProductId(any());
        verify(inboundOrderMapper, never()).insert(any(InboundOrder.class));
        verify(inboundOrderItemMapper, never()).insert(any(InboundOrderItem.class));
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

    @Test
    void updateInboundOrderDraft_shouldReplaceItemsAndUpdateHeader_whenDraft() {
        InboundOrder existing = new InboundOrder();
        existing.setId(500L);
        existing.setOrderStatus(OrderStatusConstant.INBOUND_DRAFT);

        InboundOrderAddDTO dto = new InboundOrderAddDTO();
        dto.setSupplierName("供应商A");
        dto.setRemark("更新草稿");

        InboundOrderItemAddDTO itemDTO = new InboundOrderItemAddDTO();
        itemDTO.setProductId(10L);
        itemDTO.setQuantity(3);
        itemDTO.setUnitPrice(new BigDecimal("12.50"));
        itemDTO.setRemark("明细备注");
        dto.setItemList(List.of(itemDTO));

        Supplier supplier = new Supplier();
        supplier.setId(88L);
        supplier.setStatus(1);

        Product product = new Product();
        product.setId(10L);
        product.setProductName("测试商品");
        product.setSpecification("500g");
        product.setUnit("袋");

        StockVO stock = new StockVO();
        stock.setProductId(10L);
        stock.setQuantity(100);

        when(inboundOrderMapper.selectById(500L)).thenReturn(existing);
        when(supplierMapper.selectByName("供应商A")).thenReturn(supplier);
        when(productMapper.selectById(10L)).thenReturn(product);
        when(stockMapper.selectByProductId(10L)).thenReturn(stock);
        when(inboundOrderMapper.updateDraftById(
                eq(500L),
                eq(88L),
                eq("供应商A"),
                eq(new BigDecimal("37.50")),
                eq("更新草稿"),
                eq(OrderStatusConstant.INBOUND_DRAFT)
        )).thenReturn(1);
        when(inboundOrderItemMapper.insert(any(InboundOrderItem.class))).thenReturn(1);

        String result = inboundOrderService.updateInboundOrderDraft(500L, dto);

        assertEquals("更新入库草稿成功", result);
        verify(inboundOrderItemMapper).deleteByInboundOrderId(500L);
        verify(inboundOrderItemMapper).insert(any(InboundOrderItem.class));
    }

    @Test
    void confirmInboundOrder_shouldThrow_whenOrderDoesNotExist() {
        CurrentUserContext.setRole("ADMIN");

        when(inboundOrderMapper.selectById(100L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> inboundOrderService.confirmInboundOrder(100L));

        assertEquals("入库单不存在", exception.getMessage());
        verify(stockFlowService, never()).increaseByInbound(any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmInboundOrder_shouldThrow_whenAlreadyCompleted() {
        CurrentUserContext.setRole("ADMIN");

        when(inboundOrderMapper.selectById(100L)).thenReturn(buildInboundOrder(100L, "IN100", OrderStatusConstant.INBOUND_COMPLETED, "remark"));

        BusinessException exception = assertThrows(BusinessException.class, () -> inboundOrderService.confirmInboundOrder(100L));

        assertEquals("入库单已确认入库，请勿重复操作", exception.getMessage());
        verify(stockFlowService, never()).increaseByInbound(any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmInboundOrder_shouldThrow_whenItemListIsEmpty() {
        CurrentUserContext.setRole("ADMIN");

        when(inboundOrderMapper.selectById(100L)).thenReturn(buildInboundOrder(100L, "IN100", OrderStatusConstant.INBOUND_DRAFT, "remark"));
        when(inboundOrderItemMapper.selectEntityListByInboundOrderId(100L)).thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class, () -> inboundOrderService.confirmInboundOrder(100L));

        assertEquals("入库单明细不能为空", exception.getMessage());
        verify(stockFlowService, never()).increaseByInbound(any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmInboundOrder_shouldThrow_whenUpdateStatusFails() {
        CurrentUserContext.setRole("ADMIN");
        CurrentUserContext.setUsername("tester");

        InboundOrder order = buildInboundOrder(100L, "IN100", OrderStatusConstant.INBOUND_DRAFT, "remark");
        InboundOrderItem item = new InboundOrderItem();
        item.setInboundOrderId(100L);
        item.setProductId(1L);
        item.setQuantity(2);

        when(inboundOrderMapper.selectById(100L)).thenReturn(order);
        when(inboundOrderItemMapper.selectEntityListByInboundOrderId(100L)).thenReturn(List.of(item));
        when(inboundOrderMapper.updateStatus(100L, OrderStatusConstant.INBOUND_COMPLETED, OrderStatusConstant.INBOUND_DRAFT)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> inboundOrderService.confirmInboundOrder(100L));

        assertEquals("入库单状态已变化，请刷新后重试", exception.getMessage());
        verify(stockFlowService).increaseByInbound(eq(100L), eq("IN100"), any(List.class), eq("tester"), eq(StockChangeTypeConstant.MANUAL_INBOUND), eq("remark"));
        verify(operationLogService, never()).recordSuccess(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void voidInboundOrder_shouldThrow_whenOrderDoesNotExist() {
        CurrentUserContext.setRole("ADMIN");

        when(inboundOrderMapper.selectById(200L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> inboundOrderService.voidInboundOrder(200L, "录入错误"));

        assertEquals("入库单不存在", exception.getMessage());
        verify(stockFlowService, never()).rollbackInboundOnVoid(any(), any(), any(), any(), any());
    }

    @Test
    void voidInboundOrder_shouldVoidDraft_whenDraft() {
        CurrentUserContext.setRole("ADMIN");
        CurrentUserContext.setUsername("tester");

        InboundOrder order = buildInboundOrder(200L, "IN200", OrderStatusConstant.INBOUND_DRAFT, "draft");
        when(inboundOrderMapper.selectById(200L)).thenReturn(order);
        when(inboundOrderMapper.updateStatus(200L, OrderStatusConstant.INBOUND_VOID, OrderStatusConstant.INBOUND_DRAFT)).thenReturn(1);

        String result = inboundOrderService.voidInboundOrder(200L, "录入错误");

        assertEquals("作废入库单成功", result);
        verify(stockFlowService, never()).rollbackInboundOnVoid(any(), any(), any(), any(), any());
        verify(operationLogService).recordSuccess(any(), any(), any(), eq(200L), eq("IN200"), eq("tester"), eq("作废入库单（草稿）"));
    }

    @Test
    void voidInboundOrder_shouldThrow_whenCompletedItemsEmpty() {
        CurrentUserContext.setRole("ADMIN");

        when(inboundOrderMapper.selectById(200L)).thenReturn(buildInboundOrder(200L, "IN200", OrderStatusConstant.INBOUND_COMPLETED, "done"));
        when(inboundOrderItemMapper.selectEntityListByInboundOrderId(200L)).thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class, () -> inboundOrderService.voidInboundOrder(200L, "录入错误"));

        assertEquals("入库单明细不能为空", exception.getMessage());
        verify(stockFlowService, never()).rollbackInboundOnVoid(any(), any(), any(), any(), any());
    }

    @Test
    void updateInboundOrderDraft_shouldThrow_whenOrderDoesNotExist() {
        when(inboundOrderMapper.selectById(500L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> inboundOrderService.updateInboundOrderDraft(500L, buildSingleItemOrderDto()));

        assertEquals("入库单不存在", exception.getMessage());
        verify(inboundOrderMapper, never()).updateDraftById(any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateInboundOrderDraft_shouldThrow_whenOrderIsNotDraft() {
        when(inboundOrderMapper.selectById(500L)).thenReturn(buildInboundOrder(500L, "IN500", OrderStatusConstant.INBOUND_COMPLETED, "done"));

        BusinessException exception = assertThrows(BusinessException.class, () -> inboundOrderService.updateInboundOrderDraft(500L, buildSingleItemOrderDto()));

        assertEquals("仅草稿状态入库单允许编辑", exception.getMessage());
        verify(inboundOrderMapper, never()).updateDraftById(any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateInboundOrderDraft_shouldThrow_whenUpdateHeaderFails() {
        InboundOrder existing = buildInboundOrder(500L, "IN500", OrderStatusConstant.INBOUND_DRAFT, "draft");
        InboundOrderAddDTO dto = buildSingleItemOrderDto();
        Supplier supplier = createSupplier(88L, 1);
        Product product = createProduct(10L, "商品A", "500g", "袋");
        StockVO stock = createStock(10L, 100);

        when(inboundOrderMapper.selectById(500L)).thenReturn(existing);
        when(supplierMapper.selectByName("供应商A")).thenReturn(supplier);
        when(productMapper.selectById(10L)).thenReturn(product);
        when(stockMapper.selectByProductId(10L)).thenReturn(stock);
        when(inboundOrderMapper.updateDraftById(eq(500L), eq(88L), eq("供应商A"), eq(new BigDecimal("25.00")), eq("首单"), eq(OrderStatusConstant.INBOUND_DRAFT))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> inboundOrderService.updateInboundOrderDraft(500L, dto));

        assertEquals("入库单状态已变化，请刷新后重试", exception.getMessage());
        verify(inboundOrderItemMapper, never()).deleteByInboundOrderId(any());
    }

    @Test
    void getInboundOrderPage_shouldNormalizeParamsAndReturnPage() {
        InboundOrderVO orderVO = new InboundOrderVO();
        orderVO.setId(700L);
        orderVO.setOrderNo("IN700");

        when(inboundOrderMapper.count("IN", "AI", OrderStatusConstant.INBOUND_DRAFT)).thenReturn(1L);
        when(inboundOrderMapper.selectPage("IN", "AI", OrderStatusConstant.INBOUND_DRAFT, 0, 200)).thenReturn(List.of(orderVO));

        PageResult<InboundOrderVO> result = inboundOrderService.getInboundOrderPage("IN", " ai ", OrderStatusConstant.INBOUND_DRAFT, 0, 500);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getPageNum());
        assertEquals(200, result.getPageSize());
        assertEquals(1, result.getList().size());
        assertEquals("IN700", result.getList().get(0).getOrderNo());
    }

    @Test
    void getInboundOrderPage_shouldThrow_whenSourceTypeInvalid() {
        BusinessException exception = assertThrows(BusinessException.class, () -> inboundOrderService.getInboundOrderPage("IN", "other", null, 1, 10));

        assertEquals("来源类型参数无效", exception.getMessage());
        verify(inboundOrderMapper, never()).count(any(), any(), any());
    }

    @Test
    void getInboundOrderPage_shouldThrow_whenOrderStatusInvalid() {
        BusinessException exception = assertThrows(BusinessException.class, () -> inboundOrderService.getInboundOrderPage("IN", "AI", 99, 1, 10));

        assertEquals("状态参数无效", exception.getMessage());
        verify(inboundOrderMapper, never()).count(any(), any(), any());
    }

    @Test
    void getInboundOrderDetail_shouldReturnItems_whenExists() {
        InboundOrderDetailVO detailVO = buildInboundOrderDetailVO();
        InboundOrderItemVO itemVO = new InboundOrderItemVO();
        itemVO.setProductCode("P-10");
        itemVO.setProductName("商品A");

        when(inboundOrderMapper.selectDetailById(900L)).thenReturn(detailVO);
        when(inboundOrderItemMapper.selectByInboundOrderId(900L)).thenReturn(List.of(itemVO));

        InboundOrderDetailVO result = inboundOrderService.getInboundOrderDetail(900L);

        assertEquals("IN900", result.getOrderNo());
        assertEquals(1, result.getItemList().size());
        assertEquals("P-10", result.getItemList().get(0).getProductCode());
    }

    @Test
    void getInboundOrderDetail_shouldThrow_whenNotFound() {
        when(inboundOrderMapper.selectDetailById(900L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> inboundOrderService.getInboundOrderDetail(900L));

        assertEquals("入库单不存在", exception.getMessage());
    }

    @Test
    void getDetail_shouldReturnMappedDetail_whenExists() {
        InboundOrder order = buildInboundOrder(910L, "IN910", OrderStatusConstant.INBOUND_COMPLETED, "remark");
        LocalDateTime time = LocalDateTime.of(2026, 4, 15, 10, 30, 0);
        order.setSupplierId(88L);
        order.setSupplierName("供应商A");
        order.setCreatedTime(time);
        order.setTotalAmount(new BigDecimal("25.00"));

        InboundItemVO itemVO = new InboundItemVO();
        itemVO.setProductId(10L);
        itemVO.setProductName("商品A");
        itemVO.setQuantity(new BigDecimal("2"));

        when(inboundOrderMapper.selectById(910L)).thenReturn(order);
        when(inboundOrderItemMapper.selectDetailItemsByInboundOrderId(910L)).thenReturn(List.of(itemVO));

        InboundDetailVO result = inboundOrderService.getDetail(910L);

        assertEquals(910L, result.getId());
        assertEquals("IN910", result.getInboundNo());
        assertEquals("供应商A", result.getSupplierName());
        assertEquals(time, result.getInboundTime());
        assertEquals(1, result.getItemList().size());
        assertEquals("商品A", result.getItemList().get(0).getProductName());
    }

    @Test
    void getDetail_shouldThrow_whenNotFound() {
        when(inboundOrderMapper.selectById(910L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> inboundOrderService.getDetail(910L));

        assertEquals("入库单不存在", exception.getMessage());
    }

    @Test
    void exportInboundOrderExcel_shouldExportWorkbook_whenDetailExists() throws Exception {
        InboundOrderDetailVO detailVO = buildInboundOrderDetailVO();
        InboundOrderItemVO itemVO = new InboundOrderItemVO();
        itemVO.setProductCode("P-10");
        itemVO.setProductName("商品A");
        itemVO.setSpecification("500g");
        itemVO.setUnit("袋");
        itemVO.setQuantity(2);
        itemVO.setUnitPrice(new BigDecimal("12.50"));
        itemVO.setAmount(new BigDecimal("25.00"));
        itemVO.setRemark("第一行");

        when(inboundOrderMapper.selectDetailById(900L)).thenReturn(detailVO);
        when(inboundOrderItemMapper.selectByInboundOrderId(900L)).thenReturn(List.of(itemVO));

        byte[] bytes = inboundOrderService.exportInboundOrderExcel(900L);

        assertTrue(bytes.length > 0);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals("入库单", workbook.getSheetAt(0).getSheetName());
            assertEquals("入库单号", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("IN900", workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue());
            assertEquals("商品编码", workbook.getSheetAt(0).getRow(6).getCell(1).getStringCellValue());
            assertEquals("P-10", workbook.getSheetAt(0).getRow(7).getCell(1).getStringCellValue());
        }
    }

    @Test
    void exportInboundOrderPdf_shouldReturnPdfBytes_whenDetailExists() {
        InboundOrderDetailVO detailVO = buildInboundOrderDetailVO();
        InboundOrderItemVO itemVO = new InboundOrderItemVO();
        itemVO.setProductCode("P-10");
        itemVO.setProductName("商品A");
        itemVO.setSpecification("500g");
        itemVO.setUnit("袋");
        itemVO.setQuantity(2);
        itemVO.setUnitPrice(new BigDecimal("12.50"));
        itemVO.setAmount(new BigDecimal("25.00"));
        itemVO.setRemark("第一行");

        when(inboundOrderMapper.selectDetailById(900L)).thenReturn(detailVO);
        when(inboundOrderItemMapper.selectByInboundOrderId(900L)).thenReturn(List.of(itemVO));

        byte[] bytes = inboundOrderService.exportInboundOrderPdf(900L);

        assertNotNull(bytes);
        assertTrue(bytes.length > 4);
        assertEquals("%PDF", new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
    }

    private InboundOrderAddDTO buildSingleItemOrderDto() {
        InboundOrderAddDTO dto = new InboundOrderAddDTO();
        dto.setSupplierName("供应商A");
        dto.setRemark("首单");
        dto.setItemList(List.of(createItemDto(10L, 2, "12.50", "第一行")));
        return dto;
    }

    private InboundOrderItemAddDTO createItemDto(Long productId, int quantity, String unitPrice, String remark) {
        InboundOrderItemAddDTO item = new InboundOrderItemAddDTO();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setRemark(remark);
        return item;
    }

    private Supplier createSupplier(Long id, Integer status) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setStatus(status);
        return supplier;
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

    private InboundOrder buildInboundOrder(Long id, String orderNo, Integer orderStatus, String remark) {
        InboundOrder order = new InboundOrder();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setOrderStatus(orderStatus);
        order.setRemark(remark);
        return order;
    }

    private InboundOrderDetailVO buildInboundOrderDetailVO() {
        InboundOrderDetailVO detailVO = new InboundOrderDetailVO();
        detailVO.setId(900L);
        detailVO.setOrderNo("IN900");
        detailVO.setSupplierName("供应商A");
        detailVO.setTotalAmount(new BigDecimal("25.00"));
        detailVO.setOrderStatus(OrderStatusConstant.INBOUND_COMPLETED);
        detailVO.setSourceType("AI");
        detailVO.setAiRecordId(123L);
        detailVO.setAiTaskNo("TASK-900");
        detailVO.setAiSourceFileName("source.png");
        detailVO.setRemark("remark");
        detailVO.setCreatedTime(LocalDateTime.of(2026, 4, 15, 10, 30, 0));
        return detailVO;
    }
}
