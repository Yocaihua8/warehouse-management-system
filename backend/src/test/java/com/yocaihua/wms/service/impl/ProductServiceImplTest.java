package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.dto.ProductAddDTO;
import com.yocaihua.wms.dto.ProductUpdateDTO;
import com.yocaihua.wms.entity.Product;
import com.yocaihua.wms.mapper.InboundOrderItemMapper;
import com.yocaihua.wms.mapper.OutboundOrderItemMapper;
import com.yocaihua.wms.mapper.ProductMapper;
import com.yocaihua.wms.mapper.StockMapper;
import com.yocaihua.wms.vo.StockVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private StockMapper stockMapper;

    @Mock
    private InboundOrderItemMapper inboundOrderItemMapper;

    @Mock
    private OutboundOrderItemMapper outboundOrderItemMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void getProductPage_shouldUseDefaultPagination_whenPageParamsInvalid() {
        Product product = product(1L, "P001", "商品A", 1);
        when(productMapper.count("P001", "商品A")).thenReturn(1L);
        when(productMapper.selectPage("P001", "商品A", 0, 10)).thenReturn(List.of(product));

        PageResult<Product> result = productService.getProductPage("P001", "商品A", 0, 0);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getPageNum());
        assertEquals(10, result.getPageSize());
        assertEquals(1, result.getList().size());
        verify(productMapper).selectPage("P001", "商品A", 0, 10);
    }

    @Test
    void getProductPage_shouldClampPageSizeToMax() {
        when(productMapper.count(null, null)).thenReturn(0L);
        when(productMapper.selectPage(null, null, 0, 200)).thenReturn(List.of());

        PageResult<Product> result = productService.getProductPage(null, null, 1, 999);

        assertEquals(200, result.getPageSize());
        verify(productMapper).selectPage(null, null, 0, 200);
    }

    @Test
    void getProductById_shouldThrow_whenMissing() {
        when(productMapper.selectById(10L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.getProductById(10L));

        assertEquals("商品不存在", exception.getMessage());
    }

    @Test
    void addProduct_shouldDefaultStatusAndNormalizeJson_whenInserted() {
        ProductAddDTO dto = productAddDto();
        dto.setStatus(null);
        dto.setCustomFieldsJson("  {\"color\":\"red\"}  ");

        when(productMapper.selectByProductCode("P001")).thenReturn(null);
        doAnswer(invocation -> {
            ProductAddDTO argument = invocation.getArgument(0);
            argument.setId(100L);
            return 1;
        }).when(productMapper).insert(dto);
        when(stockMapper.insertInitStock(100L, 0, 10)).thenReturn(1);

        String result = productService.addProduct(dto);

        assertEquals("新增商品成功", result);
        assertEquals(1, dto.getStatus());
        assertEquals("{\"color\":\"red\"}", dto.getCustomFieldsJson());
        verify(stockMapper).insertInitStock(100L, 0, 10);
    }

    @Test
    void addProduct_shouldNormalizeBlankCustomFieldsToNull() {
        ProductAddDTO dto = productAddDto();
        dto.setCustomFieldsJson("   ");

        when(productMapper.selectByProductCode("P001")).thenReturn(null);
        doAnswer(invocation -> {
            ProductAddDTO argument = invocation.getArgument(0);
            argument.setId(100L);
            return 1;
        }).when(productMapper).insert(dto);
        when(stockMapper.insertInitStock(100L, 0, 10)).thenReturn(1);

        productService.addProduct(dto);

        assertNull(dto.getCustomFieldsJson());
    }

    @Test
    void addProduct_shouldThrow_whenCodeExists() {
        ProductAddDTO dto = productAddDto();
        when(productMapper.selectByProductCode("P001")).thenReturn(product(1L, "P001", "旧商品", 1));

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.addProduct(dto));

        assertEquals("商品编码已存在", exception.getMessage());
        verify(productMapper, never()).insert(any());
    }

    @Test
    void addProduct_shouldThrow_whenInsertFails() {
        ProductAddDTO dto = productAddDto();
        when(productMapper.selectByProductCode("P001")).thenReturn(null);
        when(productMapper.insert(dto)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.addProduct(dto));

        assertEquals("新增商品失败", exception.getMessage());
        verify(stockMapper, never()).insertInitStock(any(), any(), any());
    }

    @Test
    void addProduct_shouldThrow_whenGeneratedIdMissing() {
        ProductAddDTO dto = productAddDto();
        when(productMapper.selectByProductCode("P001")).thenReturn(null);
        when(productMapper.insert(dto)).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.addProduct(dto));

        assertEquals("新增商品后未获取到商品ID", exception.getMessage());
        verify(stockMapper, never()).insertInitStock(any(), any(), any());
    }

    @Test
    void addProduct_shouldThrow_whenInitStockFails() {
        ProductAddDTO dto = productAddDto();
        when(productMapper.selectByProductCode("P001")).thenReturn(null);
        doAnswer(invocation -> {
            ProductAddDTO argument = invocation.getArgument(0);
            argument.setId(100L);
            return 1;
        }).when(productMapper).insert(dto);
        when(stockMapper.insertInitStock(100L, 0, 10)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.addProduct(dto));

        assertEquals("初始化库存记录失败", exception.getMessage());
    }

    @Test
    void addProduct_shouldThrow_whenCustomFieldsJsonInvalid() {
        ProductAddDTO dto = productAddDto();
        dto.setCustomFieldsJson("{bad json}");

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.addProduct(dto));

        assertEquals("自定义字段不是合法JSON，请检查格式", exception.getMessage());
        verify(productMapper, never()).selectByProductCode(any());
    }

    @Test
    void addProduct_shouldThrow_whenCustomFieldsJsonIsNotObject() {
        ProductAddDTO dto = productAddDto();
        dto.setCustomFieldsJson("[1,2,3]");

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.addProduct(dto));

        assertEquals("自定义字段必须是JSON对象格式", exception.getMessage());
    }

    @Test
    void addProduct_shouldThrow_whenCustomFieldsJsonTooLong() {
        ProductAddDTO dto = productAddDto();
        dto.setCustomFieldsJson("x".repeat(4001));

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.addProduct(dto));

        assertEquals("自定义字段长度不能超过4000个字符", exception.getMessage());
    }

    @Test
    void updateProduct_shouldInheritExistingStatusAndNormalizeBlankJson() {
        ProductUpdateDTO dto = productUpdateDto();
        dto.setStatus(null);
        dto.setCustomFieldsJson("   ");

        Product existing = product(8L, "P008", "商品旧", 0);
        when(productMapper.selectById(8L)).thenReturn(existing);
        when(productMapper.selectByProductCode("P008")).thenReturn(existing);
        when(productMapper.updateById(dto)).thenReturn(1);

        String result = productService.updateProduct(dto);

        assertEquals("修改商品成功", result);
        assertEquals(0, dto.getStatus());
        assertNull(dto.getCustomFieldsJson());
    }

    @Test
    void updateProduct_shouldThrow_whenProductMissing() {
        ProductUpdateDTO dto = productUpdateDto();
        when(productMapper.selectById(8L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.updateProduct(dto));

        assertEquals("商品不存在", exception.getMessage());
        verify(productMapper, never()).updateById(any());
    }

    @Test
    void updateProduct_shouldThrow_whenDuplicateCodeBelongsToAnotherProduct() {
        ProductUpdateDTO dto = productUpdateDto();
        Product existing = product(8L, "P008", "商品旧", 1);
        Product duplicate = product(9L, "P009", "商品重复", 1);

        dto.setProductCode("P009");
        when(productMapper.selectById(8L)).thenReturn(existing);
        when(productMapper.selectByProductCode("P009")).thenReturn(duplicate);

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.updateProduct(dto));

        assertEquals("商品编码已存在", exception.getMessage());
        verify(productMapper, never()).updateById(any());
    }

    @Test
    void updateProduct_shouldThrow_whenStatusInvalid() {
        ProductUpdateDTO dto = productUpdateDto();
        dto.setStatus(2);
        Product existing = product(8L, "P008", "商品旧", 1);

        when(productMapper.selectById(8L)).thenReturn(existing);
        when(productMapper.selectByProductCode("P008")).thenReturn(existing);

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.updateProduct(dto));

        assertEquals("商品状态只能为启用或停用", exception.getMessage());
    }

    @Test
    void updateProduct_shouldThrow_whenCustomFieldsJsonInvalid() {
        ProductUpdateDTO dto = productUpdateDto();
        dto.setCustomFieldsJson("{bad json}");

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.updateProduct(dto));

        assertEquals("自定义字段不是合法JSON，请检查格式", exception.getMessage());
        verify(productMapper, never()).selectById(any());
    }

    @Test
    void updateProduct_shouldThrow_whenUpdateFails() {
        ProductUpdateDTO dto = productUpdateDto();
        Product existing = product(8L, "P008", "商品旧", 1);

        when(productMapper.selectById(8L)).thenReturn(existing);
        when(productMapper.selectByProductCode("P008")).thenReturn(existing);
        when(productMapper.updateById(dto)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.updateProduct(dto));

        assertEquals("修改商品失败", exception.getMessage());
    }

    @Test
    void deleteProduct_shouldThrow_whenMissing() {
        when(productMapper.selectById(10L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.deleteProduct(10L));

        assertEquals("商品不存在", exception.getMessage());
    }

    @Test
    void deleteProduct_shouldThrow_whenReferencedByInboundOrder() {
        when(productMapper.selectById(10L)).thenReturn(product(10L, "P010", "商品A", 1));
        when(inboundOrderItemMapper.countByProductId(10L)).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.deleteProduct(10L));

        assertEquals("商品已被入库单引用，不能删除", exception.getMessage());
        verify(productMapper, never()).deleteById(any());
    }

    @Test
    void deleteProduct_shouldThrow_whenReferencedByOutboundOrder() {
        when(productMapper.selectById(10L)).thenReturn(product(10L, "P010", "商品A", 1));
        when(inboundOrderItemMapper.countByProductId(10L)).thenReturn(0);
        when(outboundOrderItemMapper.countByProductId(10L)).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.deleteProduct(10L));

        assertEquals("商品已被出库单引用，不能删除", exception.getMessage());
        verify(productMapper, never()).deleteById(any());
    }

    @Test
    void deleteProduct_shouldThrow_whenStockStillExists() {
        StockVO stock = stock(10L, 5);

        when(productMapper.selectById(10L)).thenReturn(product(10L, "P010", "商品A", 1));
        when(inboundOrderItemMapper.countByProductId(10L)).thenReturn(0);
        when(outboundOrderItemMapper.countByProductId(10L)).thenReturn(0);
        when(stockMapper.selectByProductId(10L)).thenReturn(stock);

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.deleteProduct(10L));

        assertEquals("商品仍有库存，不能删除", exception.getMessage());
        verify(productMapper, never()).deleteById(any());
    }

    @Test
    void deleteProduct_shouldThrow_whenDeleteFails() {
        when(productMapper.selectById(10L)).thenReturn(product(10L, "P010", "商品A", 1));
        when(inboundOrderItemMapper.countByProductId(10L)).thenReturn(0);
        when(outboundOrderItemMapper.countByProductId(10L)).thenReturn(0);
        when(stockMapper.selectByProductId(10L)).thenReturn(stock(10L, 0));
        when(productMapper.deleteById(10L)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.deleteProduct(10L));

        assertEquals("删除商品失败", exception.getMessage());
        verify(stockMapper, never()).deleteByProductId(any());
    }

    @Test
    void deleteProduct_shouldDeleteStockRecord_whenProductDeleted() {
        when(productMapper.selectById(10L)).thenReturn(product(10L, "P010", "商品A", 1));
        when(inboundOrderItemMapper.countByProductId(10L)).thenReturn(0);
        when(outboundOrderItemMapper.countByProductId(10L)).thenReturn(0);
        when(stockMapper.selectByProductId(10L)).thenReturn(stock(10L, 0));
        when(productMapper.deleteById(10L)).thenReturn(1);

        String result = productService.deleteProduct(10L);

        assertEquals("删除商品成功", result);
        verify(stockMapper).deleteByProductId(10L);
    }

    private ProductAddDTO productAddDto() {
        ProductAddDTO dto = new ProductAddDTO();
        dto.setProductCode("P001");
        dto.setProductName("商品A");
        dto.setSalePrice(new BigDecimal("12.50"));
        return dto;
    }

    private ProductUpdateDTO productUpdateDto() {
        ProductUpdateDTO dto = new ProductUpdateDTO();
        dto.setId(8L);
        dto.setProductCode("P008");
        dto.setProductName("商品新");
        dto.setSalePrice(new BigDecimal("18.80"));
        return dto;
    }

    private Product product(Long id, String code, String name, Integer status) {
        Product product = new Product();
        product.setId(id);
        product.setProductCode(code);
        product.setProductName(name);
        product.setStatus(status);
        return product;
    }

    private StockVO stock(Long productId, Integer quantity) {
        StockVO stock = new StockVO();
        stock.setProductId(productId);
        stock.setQuantity(quantity);
        return stock;
    }
}
