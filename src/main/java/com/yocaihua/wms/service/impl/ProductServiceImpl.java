package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.dto.ProductAddDTO;
import com.yocaihua.wms.dto.ProductUpdateDTO;
import com.yocaihua.wms.service.ProductService;
import com.yocaihua.wms.entity.Product;
import com.yocaihua.wms.mapper.ProductMapper;
import com.yocaihua.wms.mapper.InboundOrderItemMapper;
import com.yocaihua.wms.mapper.OutboundOrderItemMapper;
import com.yocaihua.wms.mapper.StockMapper;
import com.yocaihua.wms.vo.StockVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class ProductServiceImpl implements ProductService {

    private static final int DEFAULT_INIT_STOCK = 0;
    private static final int DEFAULT_WARNING_QUANTITY = 10;

    private final ProductMapper productMapper;
    private final StockMapper stockMapper;
    private final InboundOrderItemMapper inboundOrderItemMapper;
    private final OutboundOrderItemMapper outboundOrderItemMapper;

    public ProductServiceImpl(ProductMapper productMapper,
                              StockMapper stockMapper,
                              InboundOrderItemMapper inboundOrderItemMapper,
                              OutboundOrderItemMapper outboundOrderItemMapper) {
        this.productMapper = productMapper;
        this.stockMapper = stockMapper;
        this.inboundOrderItemMapper = inboundOrderItemMapper;
        this.outboundOrderItemMapper = outboundOrderItemMapper;
    }

    @Override
    public PageResult<Product> getProductPage(String productName, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        int offset = (pageNum - 1) * pageSize;

        Long total = productMapper.count(productName);
        List<Product> list = productMapper.selectPage(productName, offset, pageSize);

        return new PageResult<>(total, pageNum, pageSize, list);
    }

    @Override
    @Transactional
    public String addProduct(ProductAddDTO productAddDTO) {
        Product existingProduct = productMapper.selectByProductCode(productAddDTO.getProductCode());
        if (existingProduct != null) {
            throw new BusinessException("商品编码已存在");
        }

        int productRows = productMapper.insert(productAddDTO);
        if (productRows <= 0) {
            throw new BusinessException("新增商品失败");
        }

        Long productId = productAddDTO.getId();
        if (productId == null) {
            throw new BusinessException("新增商品后未获取到商品ID");
        }

        int stockRows = stockMapper.insertInitStock(
                productId,
                DEFAULT_INIT_STOCK,
                DEFAULT_WARNING_QUANTITY
        );

        if (stockRows <= 0) {
            throw new BusinessException("初始化库存记录失败");
        }

        return "新增商品成功";
    }

    @Override
    public String deleteProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        int inboundRefCount = inboundOrderItemMapper.countByProductId(id);
        if (inboundRefCount > 0) {
            throw new BusinessException("商品已被入库单引用，不能删除");
        }

        int outboundRefCount = outboundOrderItemMapper.countByProductId(id);
        if (outboundRefCount > 0) {
            throw new BusinessException("商品已被出库单引用，不能删除");
        }

        StockVO stock = stockMapper.selectByProductId(id);
        if (stock != null && stock.getQuantity() != null && stock.getQuantity() > 0) {
            throw new BusinessException("商品仍有库存，不能删除");
        }

        int rows = productMapper.deleteById(id);
        if (rows > 0) {
            return "删除商品成功";
        }

        throw new BusinessException("删除商品失败");
    }

    @Override
    public String updateProduct(ProductUpdateDTO productUpdateDTO) {
        Product existingProduct = productMapper.selectById(productUpdateDTO.getId());
        if (existingProduct == null) {
            throw new BusinessException("商品不存在");
        }

        int rows = productMapper.updateById(productUpdateDTO);
        if (rows > 0) {
            return "修改商品成功";
        }
        throw new BusinessException("修改商品失败");
    }

    @Override
    public Product getProductById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        return product;
    }
}