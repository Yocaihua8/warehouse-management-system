package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.OperatorHolder;
import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.OrderStatusConstant;
import com.yocaihua.wms.common.OperationLogActionConstant;
import com.yocaihua.wms.common.StockChangeTypeConstant;
import com.yocaihua.wms.dto.InboundOrderAddDTO;
import com.yocaihua.wms.dto.InboundOrderItemAddDTO;
import com.yocaihua.wms.entity.InboundOrder;
import com.yocaihua.wms.entity.InboundOrderItem;
import com.yocaihua.wms.entity.Supplier;
import com.yocaihua.wms.mapper.InboundOrderItemMapper;
import com.yocaihua.wms.mapper.ProductMapper;
import com.yocaihua.wms.mapper.InboundOrderMapper;
import com.yocaihua.wms.mapper.StockMapper;
import com.yocaihua.wms.mapper.SupplierMapper;
import com.yocaihua.wms.service.InboundOrderService;
import com.yocaihua.wms.service.OperationLogService;
import com.yocaihua.wms.service.StockFlowService;
import com.yocaihua.wms.vo.*;
import com.yocaihua.wms.entity.Product;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class InboundOrderServiceImpl extends AbstractOrderServiceSupport implements InboundOrderService {
    private static final String INBOUND_PDF_TEMPLATE_PATH = "reports/inbound-order-export.jrxml";

    private final InboundOrderMapper inboundOrderMapper;
    private final InboundOrderItemMapper inboundOrderItemMapper;
    private final StockMapper stockMapper;
    private final ProductMapper productMapper;
    private final SupplierMapper supplierMapper;
    private final StockFlowService stockFlowService;
    private final OperationLogService operationLogService;

    public InboundOrderServiceImpl(InboundOrderMapper inboundOrderMapper,
                                   InboundOrderItemMapper inboundOrderItemMapper,
                                   StockMapper stockMapper,
                                   ProductMapper productMapper,
                                   SupplierMapper supplierMapper,
                                   StockFlowService stockFlowService,
                                   OperationLogService operationLogService) {
        this.inboundOrderMapper = inboundOrderMapper;
        this.inboundOrderItemMapper = inboundOrderItemMapper;
        this.stockMapper = stockMapper;
        this.productMapper = productMapper;
        this.supplierMapper = supplierMapper;
        this.stockFlowService = stockFlowService;
        this.operationLogService = operationLogService;
    }

    @Override
    @Transactional
    public OrderCreatedVO saveInboundOrder(InboundOrderAddDTO inboundOrderAddDTO) {
        List<InboundOrderItemAddDTO> itemList = inboundOrderAddDTO.getItemList();
        if (itemList == null || itemList.isEmpty()) {
            throw new BusinessException("入库单明细不能为空");
        }
        String normalizedSupplierName = normalizeSupplierName(inboundOrderAddDTO.getSupplierName());
        Supplier matchedSupplier = validateSupplierForNewOrder(normalizedSupplierName);

        Set<Long> productIdSet = new HashSet<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        Map<Long, Product> selectedProductMap = new HashMap<>();

        for (InboundOrderItemAddDTO itemDTO : itemList) {
            if (!productIdSet.add(itemDTO.getProductId())) {
                throw new BusinessException("同一商品不能重复出现在入库单中");
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

            BigDecimal itemAmount = itemDTO.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
            totalAmount = totalAmount.add(itemAmount);
        }

        InboundOrder inboundOrder = new InboundOrder();
        inboundOrder.setOrderNo(generateOrderNo());
        inboundOrder.setSupplierId(matchedSupplier == null ? null : matchedSupplier.getId());
        inboundOrder.setSupplierName(normalizedSupplierName);
        inboundOrder.setTotalAmount(totalAmount);
        inboundOrder.setOrderStatus(OrderStatusConstant.INBOUND_DRAFT);
        inboundOrder.setRemark(inboundOrderAddDTO.getRemark());

        int orderRows = inboundOrderMapper.insert(inboundOrder);
        if (orderRows <= 0 || inboundOrder.getId() == null) {
            throw new BusinessException("保存入库单失败");
        }

        Long orderId = inboundOrder.getId();
        for (InboundOrderItemAddDTO itemDTO : itemList) {
            BigDecimal itemAmount = itemDTO.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
            Product product = selectedProductMap.get(itemDTO.getProductId());
            if (product == null) {
                throw new BusinessException("商品不存在，productId=" + itemDTO.getProductId());
            }

            InboundOrderItem item = new InboundOrderItem();
            item.setInboundOrderId(orderId);
            item.setProductId(itemDTO.getProductId());
            item.setProductNameSnapshot(product.getProductName());
            item.setSpecificationSnapshot(product.getSpecification());
            item.setUnitSnapshot(product.getUnit());
            item.setQuantity(itemDTO.getQuantity());
            item.setUnitPrice(itemDTO.getUnitPrice());
            item.setAmount(itemAmount);
            item.setRemark(itemDTO.getRemark());

            int itemRows = inboundOrderItemMapper.insert(item);
            if (itemRows <= 0) {
                throw new BusinessException("保存入库单明细失败");
            }
        }

        return new OrderCreatedVO(inboundOrder.getId(), inboundOrder.getOrderNo());
    }

    @Override
    @Transactional
    public String updateInboundOrderDraft(Long id, InboundOrderAddDTO inboundOrderAddDTO) {
        InboundOrder existingOrder = inboundOrderMapper.selectById(id);
        if (existingOrder == null) {
            throw new BusinessException("入库单不存在");
        }
        if (!OrderStatusConstant.INBOUND_DRAFT.equals(existingOrder.getOrderStatus())) {
            throw new BusinessException("仅草稿状态入库单允许编辑");
        }

        List<InboundOrderItemAddDTO> itemList = inboundOrderAddDTO.getItemList();
        if (itemList == null || itemList.isEmpty()) {
            throw new BusinessException("入库单明细不能为空");
        }
        String normalizedSupplierName = normalizeSupplierName(inboundOrderAddDTO.getSupplierName());
        Supplier matchedSupplier = validateSupplierForNewOrder(normalizedSupplierName);

        Set<Long> productIdSet = new HashSet<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        Map<Long, Product> selectedProductMap = new HashMap<>();

        for (InboundOrderItemAddDTO itemDTO : itemList) {
            if (!productIdSet.add(itemDTO.getProductId())) {
                throw new BusinessException("同一商品不能重复出现在入库单中");
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

            BigDecimal itemAmount = itemDTO.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
            totalAmount = totalAmount.add(itemAmount);
        }

        int updateRows = inboundOrderMapper.updateDraftById(
                id,
                matchedSupplier == null ? null : matchedSupplier.getId(),
                normalizedSupplierName,
                totalAmount,
                inboundOrderAddDTO.getRemark(),
                OrderStatusConstant.INBOUND_DRAFT
        );
        if (updateRows <= 0) {
            throw new BusinessException("入库单状态已变化，请刷新后重试");
        }

        inboundOrderItemMapper.deleteByInboundOrderId(id);

        for (InboundOrderItemAddDTO itemDTO : itemList) {
            Product product = selectedProductMap.get(itemDTO.getProductId());
            if (product == null) {
                throw new BusinessException("商品不存在，productId=" + itemDTO.getProductId());
            }

            BigDecimal itemAmount = itemDTO.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemDTO.getQuantity()));

            InboundOrderItem item = new InboundOrderItem();
            item.setInboundOrderId(id);
            item.setProductId(itemDTO.getProductId());
            item.setProductNameSnapshot(product.getProductName());
            item.setSpecificationSnapshot(product.getSpecification());
            item.setUnitSnapshot(product.getUnit());
            item.setQuantity(itemDTO.getQuantity());
            item.setUnitPrice(itemDTO.getUnitPrice());
            item.setAmount(itemAmount);
            item.setRemark(itemDTO.getRemark());

            int itemRows = inboundOrderItemMapper.insert(item);
            if (itemRows <= 0) {
                throw new BusinessException("保存入库单明细失败");
            }
        }

        return "更新入库草稿成功";
    }

    @Override
    @Transactional
    public String confirmInboundOrder(Long id) {
        ensureAdminPermission("确认入库");
        InboundOrder inboundOrder = inboundOrderMapper.selectById(id);
        if (inboundOrder == null) {
            throw new BusinessException("入库单不存在");
        }
        if (OrderStatusConstant.INBOUND_COMPLETED.equals(inboundOrder.getOrderStatus())) {
            throw new BusinessException("入库单已确认入库，请勿重复操作");
        }
        if (OrderStatusConstant.INBOUND_VOID.equals(inboundOrder.getOrderStatus())) {
            throw new BusinessException("已作废入库单不能确认入库");
        }
        if (!OrderStatusConstant.INBOUND_DRAFT.equals(inboundOrder.getOrderStatus())) {
            throw new BusinessException("当前入库单状态不允许确认入库");
        }

        List<InboundOrderItem> itemList = inboundOrderItemMapper.selectEntityListByInboundOrderId(id);
        if (itemList == null || itemList.isEmpty()) {
            throw new BusinessException("入库单明细不能为空");
        }

        String operator = OperatorHolder.getCurrentOperator();

        stockFlowService.increaseByInbound(
                inboundOrder.getId(),
                inboundOrder.getOrderNo(),
                itemList,
                operator,
                StockChangeTypeConstant.MANUAL_INBOUND,
                inboundOrder.getRemark()
        );

        int rows = inboundOrderMapper.updateStatus(
                id,
                OrderStatusConstant.INBOUND_COMPLETED,
                OrderStatusConstant.INBOUND_DRAFT
        );
        if (rows <= 0) {
            throw new BusinessException("入库单状态已变化，请刷新后重试");
        }
        operationLogService.recordSuccess(
                OperationLogActionConstant.INBOUND_CONFIRM,
                "入库管理",
                "INBOUND_ORDER",
                inboundOrder.getId(),
                inboundOrder.getOrderNo(),
                operator,
                "确认入库"
        );

        return "确认入库成功";
    }

    @Override
    @Transactional
    public String voidInboundOrder(Long id, String voidReason) {
        ensureAdminPermission("作废入库单");
        InboundOrder inboundOrder = inboundOrderMapper.selectById(id);
        if (inboundOrder == null) {
            throw new BusinessException("入库单不存在");
        }
        String normalizedVoidReason = normalizeVoidReason(voidReason);
        if (OrderStatusConstant.INBOUND_VOID.equals(inboundOrder.getOrderStatus())) {
            throw new BusinessException("入库单已作废，请勿重复操作");
        }

        if (OrderStatusConstant.INBOUND_DRAFT.equals(inboundOrder.getOrderStatus())) {
            int rows = inboundOrderMapper.updateStatus(
                    id,
                    OrderStatusConstant.INBOUND_VOID,
                    OrderStatusConstant.INBOUND_DRAFT
            );
            if (rows <= 0) {
                throw new BusinessException("入库单状态已变化，请刷新后重试");
            }
            operationLogService.recordSuccess(
                    OperationLogActionConstant.INBOUND_VOID,
                    "入库管理",
                    "INBOUND_ORDER",
                    inboundOrder.getId(),
                    inboundOrder.getOrderNo(),
                    OperatorHolder.getCurrentOperator(),
                    "作废入库单（草稿）"
            );

            return "作废入库单成功";
        }

        if (OrderStatusConstant.INBOUND_COMPLETED.equals(inboundOrder.getOrderStatus())) {
            List<InboundOrderItem> itemList = inboundOrderItemMapper.selectEntityListByInboundOrderId(id);
            if (itemList == null || itemList.isEmpty()) {
                throw new BusinessException("入库单明细不能为空");
            }

            String operator = OperatorHolder.getCurrentOperator();

            stockFlowService.rollbackInboundOnVoid(
                    inboundOrder.getId(),
                    inboundOrder.getOrderNo(),
                    itemList,
                    operator,
                    buildVoidRemark(inboundOrder.getRemark(), normalizedVoidReason)
            );

            int rows = inboundOrderMapper.updateStatus(
                    id,
                    OrderStatusConstant.INBOUND_VOID,
                    OrderStatusConstant.INBOUND_COMPLETED
            );
            if (rows <= 0) {
                throw new BusinessException("入库单状态已变化，请刷新后重试");
            }
            operationLogService.recordSuccess(
                    OperationLogActionConstant.INBOUND_VOID,
                    "入库管理",
                    "INBOUND_ORDER",
                    inboundOrder.getId(),
                    inboundOrder.getOrderNo(),
                    operator,
                    "作废入库单并回退库存，原因：" + normalizedVoidReason
            );

            return "作废入库单成功，库存已回退";
        }

        throw new BusinessException("当前入库单状态不允许作废");
    }

    private Supplier validateSupplierForNewOrder(String normalizedSupplierName) {
        Supplier supplier = supplierMapper.selectByName(normalizedSupplierName);
        if (supplier != null && !Integer.valueOf(1).equals(supplier.getStatus())) {
            throw new BusinessException("供应商已停用，不能用于新入库单");
        }
        return supplier;
    }

    private String normalizeSupplierName(String supplierName) {
        if (supplierName == null || supplierName.trim().isEmpty()) {
            throw new BusinessException("供应商名称不能为空");
        }
        return supplierName.trim();
    }

    private String generateOrderNo() {
        return "IN" + System.currentTimeMillis();
    }

    @Override
    public PageResult<InboundOrderVO> getInboundOrderPage(String orderNo, String sourceType, Integer orderStatus, Integer pageNum, Integer pageSize) {
        pageNum = normalizePageNum(pageNum);
        pageSize = normalizePageSize(pageSize);

        int offset = (pageNum - 1) * pageSize;
        String normalizedSourceType = normalizeSourceType(sourceType);
        Integer normalizedOrderStatus = normalizeOrderStatus(orderStatus);

        Long total = inboundOrderMapper.count(orderNo, normalizedSourceType, normalizedOrderStatus);
        List<InboundOrderVO> list = inboundOrderMapper.selectPage(orderNo, normalizedSourceType, normalizedOrderStatus, offset, pageSize);

        return new PageResult<>(total, pageNum, pageSize, list);
    }

    private String normalizeSourceType(String sourceType) {
        if (sourceType == null || sourceType.trim().isEmpty()) {
            return null;
        }
        String normalized = sourceType.trim().toUpperCase();
        if (!"AI".equals(normalized) && !"MANUAL".equals(normalized)) {
            throw new BusinessException("来源类型参数无效");
        }
        return normalized;
    }

    private Integer normalizeOrderStatus(Integer orderStatus) {
        if (orderStatus == null) {
            return null;
        }
        if (!OrderStatusConstant.INBOUND_DRAFT.equals(orderStatus)
                && !OrderStatusConstant.INBOUND_COMPLETED.equals(orderStatus)
                && !OrderStatusConstant.INBOUND_VOID.equals(orderStatus)) {
            throw new BusinessException("状态参数无效");
        }
        return orderStatus;
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

    @Override
    public byte[] exportInboundOrderExcel(Long id) {
        InboundOrderDetailVO detailVO = getInboundOrderDetail(id);
        List<InboundOrderItemVO> items = detailVO.getItemList();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("入库单");

            int rowIndex = 0;
            rowIndex = writeSummaryRow(sheet, rowIndex, "入库单号", detailVO.getOrderNo(), "供应商", detailVO.getSupplierName());
            rowIndex = writeSummaryRow(sheet, rowIndex, "总金额", formatAmount(detailVO.getTotalAmount()), "来源类型", resolveSourceTypeText(detailVO.getSourceType()));
            rowIndex = writeSummaryRow(sheet, rowIndex, "状态", resolveInboundOrderStatusText(detailVO.getOrderStatus()), "创建时间", formatTime(detailVO.getCreatedTime()));
            rowIndex = writeSummaryRow(sheet, rowIndex, "备注", detailVO.getRemark(), "AI记录ID", detailVO.getAiRecordId() == null ? "-" : String.valueOf(detailVO.getAiRecordId()));
            rowIndex = writeSummaryRow(sheet, rowIndex, "AI任务号", detailVO.getAiTaskNo(), "源文件名", detailVO.getAiSourceFileName());

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
                    InboundOrderItemVO item = items.get(i);
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
            throw new BusinessException("导出入库单Excel失败");
        }
    }

    @Override
    public byte[] exportInboundOrderPdf(Long id) {
        InboundOrderDetailVO detailVO = getInboundOrderDetail(id);
        try {
            JasperReport report = compileReport(INBOUND_PDF_TEMPLATE_PATH);
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("orderNo", defaultText(detailVO.getOrderNo()));
            params.put("supplierName", defaultText(detailVO.getSupplierName()));
            params.put("sourceTypeText", resolveSourceTypeText(detailVO.getSourceType()));
            params.put("orderStatusText", resolveInboundOrderStatusText(detailVO.getOrderStatus()));
            params.put("createdTime", formatTime(detailVO.getCreatedTime()));
            params.put("totalAmountText", formatAmount(detailVO.getTotalAmount()));
            params.put("remark", defaultText(detailVO.getRemark()));
            params.put("aiRecordId", detailVO.getAiRecordId() == null ? "-" : String.valueOf(detailVO.getAiRecordId()));
            params.put("aiTaskNo", defaultText(detailVO.getAiTaskNo()));
            params.put("aiSourceFileName", defaultText(detailVO.getAiSourceFileName()));
            List<InboundOrderItemVO> itemList = detailVO.getItemList() == null ? List.of() : detailVO.getItemList();
            JasperPrint print = JasperFillManager.fillReport(report, params, new JRBeanCollectionDataSource(itemList));
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException ex) {
            throw new BusinessException("导出入库单PDF失败");
        }
    }

    @Override
    public InboundDetailVO getDetail(Long id) {
        InboundOrder inboundOrder = inboundOrderMapper.selectById(id);
        if (inboundOrder == null) {
            throw new BusinessException("入库单不存在");
        }

        List<InboundItemVO> itemList = inboundOrderItemMapper.selectDetailItemsByInboundOrderId(id);

        InboundDetailVO detailVO = new InboundDetailVO();
        detailVO.setId(inboundOrder.getId());
        detailVO.setInboundNo(inboundOrder.getOrderNo());
        detailVO.setSupplierId(inboundOrder.getSupplierId());
        detailVO.setSupplierName(inboundOrder.getSupplierName());
        detailVO.setInboundTime(inboundOrder.getCreatedTime());
        detailVO.setStatus(inboundOrder.getOrderStatus());
        detailVO.setRemark(inboundOrder.getRemark());
        detailVO.setTotalAmount(inboundOrder.getTotalAmount());
        detailVO.setItemList(itemList);

        return detailVO;
    }

    private String resolveSourceTypeText(String sourceType) {
        return "AI".equalsIgnoreCase(sourceType) ? "AI识别生成" : "手工创建";
    }

    private String resolveInboundOrderStatusText(Integer status) {
        if (OrderStatusConstant.INBOUND_COMPLETED.equals(status)) {
            return "已入库";
        }
        if (OrderStatusConstant.INBOUND_VOID.equals(status)) {
            return "已作废";
        }
        return "草稿";
    }

}
