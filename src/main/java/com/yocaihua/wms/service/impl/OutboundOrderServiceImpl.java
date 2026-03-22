package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.OrderStatusConstant;
import com.yocaihua.wms.dto.OutboundOrderAddDTO;
import com.yocaihua.wms.dto.OutboundOrderItemAddDTO;
import com.yocaihua.wms.entity.Customer;
import com.yocaihua.wms.entity.OutboundOrder;
import com.yocaihua.wms.entity.OutboundOrderItem;
import com.yocaihua.wms.entity.Product;
import com.yocaihua.wms.mapper.CustomerMapper;
import com.yocaihua.wms.mapper.OutboundOrderMapper;
import com.yocaihua.wms.mapper.ProductMapper;
import com.yocaihua.wms.mapper.StockMapper;
import com.yocaihua.wms.mapper.OutboundOrderItemMapper;
import com.yocaihua.wms.service.OutboundOrderService;
import com.yocaihua.wms.vo.OutboundOrderDetailVO;
import com.yocaihua.wms.vo.OutboundOrderVO;
import com.yocaihua.wms.vo.StockVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class OutboundOrderServiceImpl implements OutboundOrderService {

    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundOrderItemMapper outboundOrderItemMapper;
    private final StockMapper stockMapper;
    private final CustomerMapper customerMapper;
    private final ProductMapper productMapper;

    public OutboundOrderServiceImpl(OutboundOrderMapper outboundOrderMapper,
                                    OutboundOrderItemMapper outboundOrderItemMapper,
                                    StockMapper stockMapper,
                                    CustomerMapper customerMapper,
                                    ProductMapper productMapper) {
        this.outboundOrderMapper = outboundOrderMapper;
        this.outboundOrderItemMapper = outboundOrderItemMapper;
        this.stockMapper = stockMapper;
        this.customerMapper = customerMapper;
        this.productMapper = productMapper;
    }

    @Override
    @Transactional
    public String saveOutboundOrder(OutboundOrderAddDTO outboundOrderAddDTO) {
        List<OutboundOrderItemAddDTO> itemList = outboundOrderAddDTO.getItemList();
        if (itemList == null || itemList.isEmpty()) {
            throw new BusinessException("出库明细不能为空");
        }

        Customer customer = customerMapper.selectById(outboundOrderAddDTO.getCustomerId());
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }

        Set<Long> productIdSet = new HashSet<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OutboundOrderItemAddDTO itemDTO : itemList) {
            if (!productIdSet.add(itemDTO.getProductId())) {
                throw new BusinessException("同一商品不能重复出现在出库单中");
            }

            Product product = productMapper.selectById(itemDTO.getProductId());
            if (product == null) {
                throw new BusinessException("商品不存在，productId=" + itemDTO.getProductId());
            }

            StockVO stock = stockMapper.selectByProductId(itemDTO.getProductId());
            if (stock == null) {
                throw new BusinessException("商品库存记录不存在，productId=" + itemDTO.getProductId());
            }

            if (stock.getQuantity() == null || stock.getQuantity() < itemDTO.getQuantity()) {
                throw new BusinessException("商品库存不足，productId=" + itemDTO.getProductId());
            }

            BigDecimal amount = itemDTO.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
            totalAmount = totalAmount.add(amount);
        }

        OutboundOrder outboundOrder = new OutboundOrder();
        outboundOrder.setOrderNo(generateOrderNo());
        outboundOrder.setCustomerId(outboundOrderAddDTO.getCustomerId());
        outboundOrder.setCreatedTime(LocalDateTime.now());
        outboundOrder.setTotalAmount(totalAmount);
        outboundOrder.setRemark(outboundOrderAddDTO.getRemark());
        outboundOrder.setOrderStatus(OrderStatusConstant.OUTBOUND_CREATED);

        int orderRows = outboundOrderMapper.insert(outboundOrder);
        if (orderRows <= 0 || outboundOrder.getId() == null) {
            throw new BusinessException("保存出库单失败");
        }

        Long orderId = outboundOrder.getId();

        for (OutboundOrderItemAddDTO itemDTO : itemList) {
            OutboundOrderItem item = new OutboundOrderItem();
            item.setOutboundOrderId(orderId);
            item.setProductId(itemDTO.getProductId());
            item.setQuantity(itemDTO.getQuantity());
            item.setUnitPrice(itemDTO.getUnitPrice());
            item.setAmount(itemDTO.getUnitPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
            item.setRemark(itemDTO.getRemark());

            int itemRows = outboundOrderItemMapper.insert(item);
            if (itemRows <= 0) {
                throw new BusinessException("保存出库单明细失败");
            }

            int stockRows = stockMapper.decreaseStock(itemDTO.getProductId(), itemDTO.getQuantity());
            if (stockRows <= 0) {
                throw new BusinessException("扣减库存失败，productId=" + itemDTO.getProductId());
            }
        }

        return "保存出库单成功";
    }

    private String generateOrderNo() {
        return "OUT" + System.currentTimeMillis();
    }

    @Override
    public PageResult<OutboundOrderVO> getOutboundOrderPage(String orderNo, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        int offset = (pageNum - 1) * pageSize;

        Long total = outboundOrderMapper.count(orderNo);
        List<OutboundOrderVO> list = outboundOrderMapper.selectPage(orderNo, offset, pageSize);

        return new PageResult<>(total, pageNum, pageSize, list);
    }

    @Override
    public OutboundOrderDetailVO getOutboundOrderDetail(Long id) {
        OutboundOrderDetailVO detailVO = outboundOrderMapper.selectDetailById(id);
        if (detailVO == null) {
            throw new BusinessException("出库单不存在");
        }

        detailVO.setItemList(outboundOrderItemMapper.selectByOutboundOrderId(id));
        return detailVO;
    }

}