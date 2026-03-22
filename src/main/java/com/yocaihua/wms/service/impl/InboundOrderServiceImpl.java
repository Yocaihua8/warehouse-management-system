package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.OrderStatusConstant;
import com.yocaihua.wms.dto.InboundOrderAddDTO;
import com.yocaihua.wms.dto.InboundOrderItemAddDTO;
import com.yocaihua.wms.entity.InboundOrder;
import com.yocaihua.wms.entity.InboundOrderItem;
import com.yocaihua.wms.mapper.InboundOrderItemMapper;
import com.yocaihua.wms.mapper.ProductMapper;
import com.yocaihua.wms.mapper.InboundOrderMapper;
import com.yocaihua.wms.mapper.StockMapper;
import com.yocaihua.wms.service.InboundOrderService;
import com.yocaihua.wms.vo.InboundOrderVO;
import com.yocaihua.wms.vo.InboundOrderDetailVO;
import com.yocaihua.wms.vo.StockVO;
import com.yocaihua.wms.entity.Product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class InboundOrderServiceImpl implements InboundOrderService {

    private final InboundOrderMapper inboundOrderMapper;
    private final InboundOrderItemMapper inboundOrderItemMapper;
    private final StockMapper stockMapper;
    private final ProductMapper productMapper;

    public InboundOrderServiceImpl(InboundOrderMapper inboundOrderMapper,
                                   InboundOrderItemMapper inboundOrderItemMapper,
                                   StockMapper stockMapper,
                                   ProductMapper productMapper) {
        this.inboundOrderMapper = inboundOrderMapper;
        this.inboundOrderItemMapper = inboundOrderItemMapper;
        this.stockMapper = stockMapper;
        this.productMapper = productMapper;
    }

    @Override
    @Transactional
    public String saveInboundOrder(InboundOrderAddDTO inboundOrderAddDTO) {
        List<InboundOrderItemAddDTO> itemList = inboundOrderAddDTO.getItemList();
        if (itemList == null || itemList.isEmpty()) {
            throw new BusinessException("入库单明细不能为空");
        }

        Set<Long> productIdSet = new HashSet<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (InboundOrderItemAddDTO itemDTO : itemList) {
            if (!productIdSet.add(itemDTO.getProductId())) {
                throw new BusinessException("同一商品不能重复出现在入库单中");
            }

            Product product = productMapper.selectById(itemDTO.getProductId());
            if (product == null) {
                throw new BusinessException("商品不存在，productId=" + itemDTO.getProductId());
            }

            StockVO stock = stockMapper.selectByProductId(itemDTO.getProductId());
            if (stock == null) {
                throw new BusinessException("商品库存记录不存在，productId=" + itemDTO.getProductId());
            }

            BigDecimal itemAmount = itemDTO.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
            totalAmount = totalAmount.add(itemAmount);
        }

        InboundOrder inboundOrder = new InboundOrder();
        inboundOrder.setOrderNo(generateOrderNo());
        inboundOrder.setSupplierName(inboundOrderAddDTO.getSupplierName());
        inboundOrder.setTotalAmount(totalAmount);
        inboundOrder.setOrderStatus(OrderStatusConstant.INBOUND_COMPLETED);
        inboundOrder.setRemark(inboundOrderAddDTO.getRemark());

        int orderRows = inboundOrderMapper.insert(inboundOrder);
        if (orderRows <= 0 || inboundOrder.getId() == null) {
            throw new BusinessException("保存入库单失败");
        }

        Long orderId = inboundOrder.getId();

        for (InboundOrderItemAddDTO itemDTO : itemList) {
            BigDecimal itemAmount = itemDTO.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemDTO.getQuantity()));

            InboundOrderItem item = new InboundOrderItem();
            item.setInboundOrderId(orderId);
            item.setProductId(itemDTO.getProductId());
            item.setQuantity(itemDTO.getQuantity());
            item.setUnitPrice(itemDTO.getUnitPrice());
            item.setAmount(itemAmount);
            item.setRemark(itemDTO.getRemark());

            int itemRows = inboundOrderItemMapper.insert(item);
            if (itemRows <= 0) {
                throw new BusinessException("保存入库单明细失败");
            }

            int stockRows = stockMapper.increaseStock(itemDTO.getProductId(), itemDTO.getQuantity());
            if (stockRows <= 0) {
                throw new BusinessException("增加库存失败，productId=" + itemDTO.getProductId());
            }
        }

        return "保存入库单成功，单号：" + inboundOrder.getOrderNo();
    }

    private String generateOrderNo() {
        return "IN" + System.currentTimeMillis();
    }

    @Override
    public PageResult<InboundOrderVO> getInboundOrderPage(String orderNo, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        int offset = (pageNum - 1) * pageSize;

        Long total = inboundOrderMapper.count(orderNo);
        List<InboundOrderVO> list = inboundOrderMapper.selectPage(orderNo, offset, pageSize);

        return new PageResult<>(total, pageNum, pageSize, list);
    }

    @Override
    public InboundOrderDetailVO getInboundOrderDetail(Long id) {
        InboundOrderDetailVO detailVO = inboundOrderMapper.selectDetailById(id);
        if (detailVO == null) {
            throw new BusinessException("入库单不存在");
        }

        detailVO.setItemList(inboundOrderItemMapper.selectByInboundOrderId(id));
        return detailVO;
    }

}