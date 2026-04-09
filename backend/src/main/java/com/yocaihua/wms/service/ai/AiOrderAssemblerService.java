package com.yocaihua.wms.service.ai;

import com.yocaihua.wms.common.OrderStatusConstant;
import com.yocaihua.wms.dto.AiInboundConfirmItemDTO;
import com.yocaihua.wms.dto.AiOutboundConfirmItemDTO;
import com.yocaihua.wms.entity.InboundOrder;
import com.yocaihua.wms.entity.InboundOrderItem;
import com.yocaihua.wms.entity.OutboundOrder;
import com.yocaihua.wms.entity.OutboundOrderItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AiOrderAssemblerService {

    public InboundOrder buildInboundOrder(Long supplierId, String supplierName, String remark) {
        InboundOrder inboundOrder = new InboundOrder();
        inboundOrder.setOrderNo(buildInboundOrderNo());
        inboundOrder.setSupplierId(supplierId);
        inboundOrder.setSupplierName(supplierName);
        inboundOrder.setOrderStatus(OrderStatusConstant.INBOUND_COMPLETED);
        inboundOrder.setRemark(remark);
        return inboundOrder;
    }

    public OutboundOrder buildOutboundOrder(Long customerId, String customerNameSnapshot, String remark) {
        OutboundOrder outboundOrder = new OutboundOrder();
        outboundOrder.setOrderNo(buildOutboundOrderNo());
        outboundOrder.setCustomerId(customerId);
        outboundOrder.setCustomerNameSnapshot(customerNameSnapshot);
        outboundOrder.setOrderStatus(OrderStatusConstant.OUTBOUND_COMPLETED);
        outboundOrder.setRemark(remark);
        return outboundOrder;
    }

    public InboundOrderItem buildInboundOrderItem(AiInboundConfirmItemDTO itemDTO, BigDecimal amount) {
        InboundOrderItem orderItem = new InboundOrderItem();
        orderItem.setProductId(itemDTO.getMatchedProductId());
        orderItem.setProductNameSnapshot(normalizeOptionalText(itemDTO.getProductName()));
        orderItem.setSpecificationSnapshot(normalizeOptionalText(itemDTO.getSpecification()));
        orderItem.setUnitSnapshot(normalizeOptionalText(itemDTO.getUnit()));
        orderItem.setQuantity(itemDTO.getQuantity());
        orderItem.setUnitPrice(itemDTO.getUnitPrice());
        orderItem.setAmount(amount);
        orderItem.setRemark(normalizeOptionalText(itemDTO.getRemark()));
        return orderItem;
    }

    public OutboundOrderItem buildOutboundOrderItem(AiOutboundConfirmItemDTO itemDTO, BigDecimal amount) {
        OutboundOrderItem orderItem = new OutboundOrderItem();
        orderItem.setProductId(itemDTO.getMatchedProductId());
        orderItem.setProductNameSnapshot(normalizeOptionalText(itemDTO.getProductName()));
        orderItem.setSpecificationSnapshot(normalizeOptionalText(itemDTO.getSpecification()));
        orderItem.setUnitSnapshot(normalizeOptionalText(itemDTO.getUnit()));
        orderItem.setQuantity(itemDTO.getQuantity());
        orderItem.setUnitPrice(itemDTO.getUnitPrice());
        orderItem.setAmount(amount);
        orderItem.setRemark(normalizeOptionalText(itemDTO.getRemark()));
        return orderItem;
    }

    public BigDecimal resolveItemAmount(BigDecimal amount, Integer quantity, BigDecimal unitPrice) {
        if (amount != null) {
            return amount;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    private String buildInboundOrderNo() {
        return "RK" + System.currentTimeMillis();
    }

    private String buildOutboundOrderNo() {
        return "OUT" + System.currentTimeMillis();
    }

    private String normalizeOptionalText(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
