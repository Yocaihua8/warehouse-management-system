package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.OperatorHolder;
import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.OrderStatusConstant;
import com.yocaihua.wms.common.OperationLogActionConstant;
import com.yocaihua.wms.common.StockChangeTypeConstant;
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
import com.yocaihua.wms.service.OperationLogService;
import com.yocaihua.wms.service.StockFlowService;
import com.yocaihua.wms.vo.OrderCreatedVO;
import com.yocaihua.wms.vo.OutboundOrderDetailVO;
import com.yocaihua.wms.vo.OutboundOrderItemVO;
import com.yocaihua.wms.vo.OutboundOrderVO;
import com.yocaihua.wms.vo.StockVO;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OutboundOrderServiceImpl extends AbstractOrderServiceSupport implements OutboundOrderService {
    private static final String OUTBOUND_PDF_TEMPLATE_PATH = "reports/outbound-order-export.jrxml";

    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundOrderItemMapper outboundOrderItemMapper;
    private final StockMapper stockMapper;
    private final CustomerMapper customerMapper;
    private final ProductMapper productMapper;
    private final StockFlowService stockFlowService;
    private final OperationLogService operationLogService;

    public OutboundOrderServiceImpl(OutboundOrderMapper outboundOrderMapper,
                                    OutboundOrderItemMapper outboundOrderItemMapper,
                                    StockMapper stockMapper,
                                    CustomerMapper customerMapper,
                                    ProductMapper productMapper,
                                    StockFlowService stockFlowService,
                                    OperationLogService operationLogService) {
        this.outboundOrderMapper = outboundOrderMapper;
        this.outboundOrderItemMapper = outboundOrderItemMapper;
        this.stockMapper = stockMapper;
        this.customerMapper = customerMapper;
        this.productMapper = productMapper;
        this.stockFlowService = stockFlowService;
        this.operationLogService = operationLogService;
    }

    @Override
    @Transactional
    public OrderCreatedVO saveOutboundOrder(OutboundOrderAddDTO outboundOrderAddDTO) {
        List<OutboundOrderItemAddDTO> itemList = outboundOrderAddDTO.getItemList();
        if (itemList == null || itemList.isEmpty()) {
            throw new BusinessException("出库明细不能为空");
        }

        Customer customer = customerMapper.selectById(outboundOrderAddDTO.getCustomerId());
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }
        ensureCustomerActive(customer);

        Set<Long> productIdSet = new HashSet<>();
        Map<Long, Product> selectedProductMap = new HashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OutboundOrderItemAddDTO itemDTO : itemList) {
            if (!productIdSet.add(itemDTO.getProductId())) {
                throw new BusinessException("同一商品不能重复出现在出库单中");
            }

            Product product = productMapper.selectById(itemDTO.getProductId());
            if (product == null) {
                throw new BusinessException("商品不存在，productId=" + itemDTO.getProductId());
            }
            selectedProductMap.put(product.getId(), product);

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
        outboundOrder.setCustomerNameSnapshot(customer.getCustomerName());
        outboundOrder.setCreatedTime(LocalDateTime.now());
        outboundOrder.setTotalAmount(totalAmount);
        outboundOrder.setRemark(outboundOrderAddDTO.getRemark());
        outboundOrder.setOrderStatus(OrderStatusConstant.OUTBOUND_DRAFT);

        int orderRows = outboundOrderMapper.insert(outboundOrder);
        if (orderRows <= 0 || outboundOrder.getId() == null) {
            throw new BusinessException("保存出库单失败");
        }

        Long orderId = outboundOrder.getId();
        for (OutboundOrderItemAddDTO itemDTO : itemList) {
            Product product = selectedProductMap.get(itemDTO.getProductId());
            if (product == null) {
                throw new BusinessException("商品不存在，productId=" + itemDTO.getProductId());
            }

            OutboundOrderItem item = new OutboundOrderItem();
            item.setOutboundOrderId(orderId);
            item.setProductId(itemDTO.getProductId());
            item.setProductNameSnapshot(product.getProductName());
            item.setSpecificationSnapshot(product.getSpecification());
            item.setUnitSnapshot(product.getUnit());
            item.setQuantity(itemDTO.getQuantity());
            item.setUnitPrice(itemDTO.getUnitPrice());
            item.setAmount(itemDTO.getUnitPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
            item.setRemark(itemDTO.getRemark());

            int itemRows = outboundOrderItemMapper.insert(item);
            if (itemRows <= 0) {
                throw new BusinessException("保存出库单明细失败");
            }
        }

        return new OrderCreatedVO(outboundOrder.getId(), outboundOrder.getOrderNo());
    }

    @Override
    @Transactional
    public String updateOutboundOrderDraft(Long id, OutboundOrderAddDTO outboundOrderAddDTO) {
        OutboundOrder existingOrder = outboundOrderMapper.selectById(id);
        if (existingOrder == null) {
            throw new BusinessException("出库单不存在");
        }
        if (!OrderStatusConstant.OUTBOUND_DRAFT.equals(existingOrder.getOrderStatus())) {
            throw new BusinessException("仅草稿状态出库单允许编辑");
        }

        List<OutboundOrderItemAddDTO> itemList = outboundOrderAddDTO.getItemList();
        if (itemList == null || itemList.isEmpty()) {
            throw new BusinessException("出库明细不能为空");
        }

        Customer customer = customerMapper.selectById(outboundOrderAddDTO.getCustomerId());
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }
        ensureCustomerActive(customer);

        Set<Long> productIdSet = new HashSet<>();
        Map<Long, Product> selectedProductMap = new HashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OutboundOrderItemAddDTO itemDTO : itemList) {
            if (!productIdSet.add(itemDTO.getProductId())) {
                throw new BusinessException("同一商品不能重复出现在出库单中");
            }

            Product product = productMapper.selectById(itemDTO.getProductId());
            if (product == null) {
                throw new BusinessException("商品不存在，productId=" + itemDTO.getProductId());
            }
            selectedProductMap.put(product.getId(), product);

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

        int updateRows = outboundOrderMapper.updateDraftById(
                id,
                outboundOrderAddDTO.getCustomerId(),
                customer.getCustomerName(),
                totalAmount,
                outboundOrderAddDTO.getRemark(),
                OrderStatusConstant.OUTBOUND_DRAFT
        );
        if (updateRows <= 0) {
            throw new BusinessException("出库单状态已变化，请刷新后重试");
        }

        outboundOrderItemMapper.deleteByOutboundOrderId(id);

        for (OutboundOrderItemAddDTO itemDTO : itemList) {
            Product product = selectedProductMap.get(itemDTO.getProductId());
            if (product == null) {
                throw new BusinessException("商品不存在，productId=" + itemDTO.getProductId());
            }

            OutboundOrderItem item = new OutboundOrderItem();
            item.setOutboundOrderId(id);
            item.setProductId(itemDTO.getProductId());
            item.setProductNameSnapshot(product.getProductName());
            item.setSpecificationSnapshot(product.getSpecification());
            item.setUnitSnapshot(product.getUnit());
            item.setQuantity(itemDTO.getQuantity());
            item.setUnitPrice(itemDTO.getUnitPrice());
            item.setAmount(itemDTO.getUnitPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
            item.setRemark(itemDTO.getRemark());

            int itemRows = outboundOrderItemMapper.insert(item);
            if (itemRows <= 0) {
                throw new BusinessException("保存出库单明细失败");
            }
        }

        return "更新出库草稿成功";
    }

    @Override
    @Transactional
    public String confirmOutboundOrder(Long id) {
        ensureAdminPermission("确认出库");
        OutboundOrder outboundOrder = outboundOrderMapper.selectById(id);
        if (outboundOrder == null) {
            throw new BusinessException("出库单不存在");
        }
        if (OrderStatusConstant.OUTBOUND_COMPLETED.equals(outboundOrder.getOrderStatus())) {
            throw new BusinessException("出库单已确认出库，请勿重复操作");
        }
        if (OrderStatusConstant.OUTBOUND_VOID.equals(outboundOrder.getOrderStatus())) {
            throw new BusinessException("已作废出库单不能确认出库");
        }
        if (!OrderStatusConstant.OUTBOUND_DRAFT.equals(outboundOrder.getOrderStatus())) {
            throw new BusinessException("当前出库单状态不允许确认出库");
        }

        List<OutboundOrderItem> itemList = outboundOrderItemMapper.selectEntityListByOutboundOrderId(id);
        if (itemList == null || itemList.isEmpty()) {
            throw new BusinessException("出库单明细不能为空");
        }

        String operator = OperatorHolder.getCurrentOperator();

        stockFlowService.decreaseByOutbound(
                outboundOrder.getId(),
                outboundOrder.getOrderNo(),
                itemList,
                operator,
                StockChangeTypeConstant.MANUAL_OUTBOUND,
                outboundOrder.getRemark()
        );

        int rows = outboundOrderMapper.updateStatus(
                id,
                OrderStatusConstant.OUTBOUND_COMPLETED,
                OrderStatusConstant.OUTBOUND_DRAFT
        );
        if (rows <= 0) {
            throw new BusinessException("出库单状态已变化，请刷新后重试");
        }
        operationLogService.recordSuccess(
                OperationLogActionConstant.OUTBOUND_CONFIRM,
                "出库管理",
                "OUTBOUND_ORDER",
                outboundOrder.getId(),
                outboundOrder.getOrderNo(),
                operator,
                "确认出库"
        );

        return "确认出库成功";
    }

    @Override
    @Transactional
    public String voidOutboundOrder(Long id, String voidReason) {
        ensureAdminPermission("作废出库单");
        OutboundOrder outboundOrder = outboundOrderMapper.selectById(id);
        if (outboundOrder == null) {
            throw new BusinessException("出库单不存在");
        }
        String normalizedVoidReason = normalizeVoidReason(voidReason);
        if (OrderStatusConstant.OUTBOUND_VOID.equals(outboundOrder.getOrderStatus())) {
            throw new BusinessException("出库单已作废，请勿重复操作");
        }

        if (OrderStatusConstant.OUTBOUND_DRAFT.equals(outboundOrder.getOrderStatus())) {
            int rows = outboundOrderMapper.updateStatus(
                    id,
                    OrderStatusConstant.OUTBOUND_VOID,
                    OrderStatusConstant.OUTBOUND_DRAFT
            );
            if (rows <= 0) {
                throw new BusinessException("出库单状态已变化，请刷新后重试");
            }
            operationLogService.recordSuccess(
                    OperationLogActionConstant.OUTBOUND_VOID,
                    "出库管理",
                    "OUTBOUND_ORDER",
                    outboundOrder.getId(),
                    outboundOrder.getOrderNo(),
                    OperatorHolder.getCurrentOperator(),
                    "作废出库单（草稿）"
            );

            return "作废出库单成功";
        }

        if (OrderStatusConstant.OUTBOUND_COMPLETED.equals(outboundOrder.getOrderStatus())) {
            List<OutboundOrderItem> itemList = outboundOrderItemMapper.selectEntityListByOutboundOrderId(id);
            if (itemList == null || itemList.isEmpty()) {
                throw new BusinessException("出库单明细不能为空");
            }

            String operator = OperatorHolder.getCurrentOperator();

            stockFlowService.rollbackOutboundOnVoid(
                    outboundOrder.getId(),
                    outboundOrder.getOrderNo(),
                    itemList,
                    operator,
                    buildVoidRemark(outboundOrder.getRemark(), normalizedVoidReason)
            );

            int rows = outboundOrderMapper.updateStatus(
                    id,
                    OrderStatusConstant.OUTBOUND_VOID,
                    OrderStatusConstant.OUTBOUND_COMPLETED
            );
            if (rows <= 0) {
                throw new BusinessException("出库单状态已变化，请刷新后重试");
            }
            operationLogService.recordSuccess(
                    OperationLogActionConstant.OUTBOUND_VOID,
                    "出库管理",
                    "OUTBOUND_ORDER",
                    outboundOrder.getId(),
                    outboundOrder.getOrderNo(),
                    operator,
                    "作废出库单并回补库存，原因：" + normalizedVoidReason
            );

            return "作废出库单成功，库存已回补";
        }

        throw new BusinessException("当前出库单状态不允许作废");
    }

    private void ensureCustomerActive(Customer customer) {
        if (!Integer.valueOf(1).equals(customer.getStatus())) {
            throw new BusinessException("客户已停用，不能用于新出库单");
        }
    }

    private String generateOrderNo() {
        return "OUT" + System.currentTimeMillis();
    }

    @Override
    public PageResult<OutboundOrderVO> getOutboundOrderPage(String orderNo, Integer orderStatus, Integer pageNum, Integer pageSize) {
        pageNum = normalizePageNum(pageNum);
        pageSize = normalizePageSize(pageSize);

        int offset = (pageNum - 1) * pageSize;
        Integer normalizedOrderStatus = normalizeOrderStatus(orderStatus);

        Long total = outboundOrderMapper.count(orderNo, normalizedOrderStatus);
        List<OutboundOrderVO> list = outboundOrderMapper.selectPage(orderNo, normalizedOrderStatus, offset, pageSize);

        return new PageResult<>(total, pageNum, pageSize, list);
    }

    private Integer normalizeOrderStatus(Integer orderStatus) {
        if (orderStatus == null) {
            return null;
        }
        if (!OrderStatusConstant.OUTBOUND_DRAFT.equals(orderStatus)
                && !OrderStatusConstant.OUTBOUND_COMPLETED.equals(orderStatus)
                && !OrderStatusConstant.OUTBOUND_VOID.equals(orderStatus)) {
            throw new BusinessException("状态参数无效");
        }
        return orderStatus;
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

    @Override
    public byte[] exportOutboundOrderExcel(Long id) {
        OutboundOrderDetailVO detailVO = getOutboundOrderDetail(id);
        List<OutboundOrderItemVO> items = detailVO.getItemList();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("出库单");

            int rowIndex = 0;
            rowIndex = writeSummaryRow(sheet, rowIndex, "出库单号", detailVO.getOrderNo(), "客户", detailVO.getCustomerName());
            rowIndex = writeSummaryRow(sheet, rowIndex, "总金额", formatAmount(detailVO.getTotalAmount()), "状态", resolveOutboundOrderStatusText(detailVO.getOrderStatus()));
            rowIndex = writeSummaryRow(sheet, rowIndex, "创建时间", formatTime(detailVO.getCreatedTime()), "备注", detailVO.getRemark());

            rowIndex += 1;
            Row headerRow = sheet.createRow(rowIndex++);
            headerRow.createCell(0).setCellValue("序号");
            headerRow.createCell(1).setCellValue("商品编码");
            headerRow.createCell(2).setCellValue("商品名称");
            headerRow.createCell(3).setCellValue("规格");
            headerRow.createCell(4).setCellValue("单位");
            headerRow.createCell(5).setCellValue("数量");
            headerRow.createCell(6).setCellValue("单价");
            headerRow.createCell(7).setCellValue("金额");
            headerRow.createCell(8).setCellValue("备注");

            if (items != null) {
                for (int i = 0; i < items.size(); i++) {
                    OutboundOrderItemVO item = items.get(i);
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(i + 1);
                    row.createCell(1).setCellValue(defaultText(item.getProductCode()));
                    row.createCell(2).setCellValue(defaultText(item.getProductName()));
                    row.createCell(3).setCellValue(defaultText(item.getSpecification()));
                    row.createCell(4).setCellValue(defaultText(item.getUnit()));
                    row.createCell(5).setCellValue(item.getQuantity() == null ? 0 : item.getQuantity());
                    row.createCell(6).setCellValue(formatAmount(item.getUnitPrice()));
                    row.createCell(7).setCellValue(formatAmount(item.getAmount()));
                    row.createCell(8).setCellValue(defaultText(item.getRemark()));
                }
            }

            for (int columnIndex = 0; columnIndex < 9; columnIndex++) {
                sheet.autoSizeColumn(columnIndex);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("导出出库单Excel失败");
        }
    }

    @Override
    public byte[] exportOutboundOrderPdf(Long id) {
        OutboundOrderDetailVO detailVO = getOutboundOrderDetail(id);
        try {
            JasperReport report = compileReport(OUTBOUND_PDF_TEMPLATE_PATH);
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("orderNo", defaultText(detailVO.getOrderNo()));
            params.put("customerName", defaultText(detailVO.getCustomerName()));
            params.put("orderStatusText", resolveOutboundOrderStatusText(detailVO.getOrderStatus()));
            params.put("createdTime", formatTime(detailVO.getCreatedTime()));
            params.put("totalAmountText", formatAmount(detailVO.getTotalAmount()));
            params.put("remark", defaultText(detailVO.getRemark()));
            List<OutboundOrderItemVO> itemList = detailVO.getItemList() == null ? List.of() : detailVO.getItemList();
            JasperPrint print = JasperFillManager.fillReport(report, params, new JRBeanCollectionDataSource(itemList));
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException ex) {
            throw new BusinessException("导出出库单PDF失败");
        }
    }

    private String resolveOutboundOrderStatusText(Integer status) {
        if (OrderStatusConstant.OUTBOUND_COMPLETED.equals(status)) {
            return "已出库";
        }
        if (OrderStatusConstant.OUTBOUND_VOID.equals(status)) {
            return "已作废";
        }
        return "草稿";
    }

}
