package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.OperatorHolder;
import com.yocaihua.wms.dto.StockUpdateDTO;
import com.yocaihua.wms.mapper.StockMapper;
import com.yocaihua.wms.service.StockFlowService;
import com.yocaihua.wms.service.StockService;
import com.yocaihua.wms.vo.StockVO;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class StockServiceImpl implements StockService {

    private static final int MAX_PAGE_SIZE = 200;

    private final StockMapper stockMapper;
    private final StockFlowService stockFlowService;

    public StockServiceImpl(StockMapper stockMapper,
                            StockFlowService stockFlowService) {
        this.stockMapper = stockMapper;
        this.stockFlowService = stockFlowService;
    }

    @Override
    public PageResult<StockVO> getStockPage(String productCode, String productName, Integer pageNum, Integer pageSize) {
        int currentPage = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int currentSize = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        if (currentSize > MAX_PAGE_SIZE) {
            currentSize = MAX_PAGE_SIZE;
        }
        int offset = (currentPage - 1) * currentSize;

        Long total = stockMapper.countStock(productCode, productName);
        List<StockVO> records = stockMapper.selectStockPage(productCode, productName, offset, currentSize);

        return new PageResult<>(total, currentPage, currentSize, records);
    }

    @Override
    @Transactional
    public String updateStock(StockUpdateDTO stockUpdateDTO) {
        StockVO stockVO = stockMapper.selectByProductId(stockUpdateDTO.getProductId());
        if (stockVO == null) {
            throw new BusinessException("库存记录不存在");
        }

        Integer beforeQuantity = stockVO.getQuantity() == null ? 0 : stockVO.getQuantity();
        Integer afterQuantity = stockUpdateDTO.getQuantity();

        if (afterQuantity == null) {
            throw new BusinessException("库存数量不能为空");
        }

        Integer warningQuantity = stockUpdateDTO.getWarningQuantity();
        if (warningQuantity == null) {
            throw new BusinessException("预警库存不能为空");
        }

        int rows = stockMapper.updateByProductId(stockUpdateDTO);
        if (rows <= 0) {
            throw new BusinessException("修改库存失败");
        }

        String operator = OperatorHolder.getCurrentOperator();
        String reason = resolveAdjustReason(stockUpdateDTO.getReason());

        stockFlowService.recordManualAdjust(
                stockUpdateDTO.getProductId(),
                stockVO.getProductName(),
                beforeQuantity,
                afterQuantity,
                operator,
                reason,
                "库存管理页手工修改"
        );

        return "修改库存成功";
    }

    @Override
    public byte[] exportStockExcel(String productCode, String productName) {
        List<StockVO> records = stockMapper.selectStockList(productCode, productName);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("库存列表");

            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("商品ID");
            headerRow.createCell(1).setCellValue("商品编码");
            headerRow.createCell(2).setCellValue("商品名称");
            headerRow.createCell(3).setCellValue("规格");
            headerRow.createCell(4).setCellValue("单位");
            headerRow.createCell(5).setCellValue("分类");
            headerRow.createCell(6).setCellValue("当前库存");
            headerRow.createCell(7).setCellValue("预警值");
            headerRow.createCell(8).setCellValue("低库存");

            int rowIndex = 1;
            for (StockVO item : records) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(item.getProductId() == null ? "" : String.valueOf(item.getProductId()));
                row.createCell(1).setCellValue(defaultText(item.getProductCode()));
                row.createCell(2).setCellValue(defaultText(item.getProductName()));
                row.createCell(3).setCellValue(defaultText(item.getSpecification()));
                row.createCell(4).setCellValue(defaultText(item.getUnit()));
                row.createCell(5).setCellValue(defaultText(item.getCategory()));
                row.createCell(6).setCellValue(item.getQuantity() == null ? 0 : item.getQuantity());
                row.createCell(7).setCellValue(item.getWarningQuantity() == null ? 0 : item.getWarningQuantity());
                row.createCell(8).setCellValue(resolveLowStockText(item.getLowStock()));
            }

            for (int columnIndex = 0; columnIndex < 9; columnIndex++) {
                sheet.autoSizeColumn(columnIndex);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("导出库存列表Excel失败");
        }
    }

    @Override
    public byte[] exportStockCsv(String productCode, String productName) {
        List<StockVO> records = stockMapper.selectStockList(productCode, productName);

        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("商品ID,商品编码,商品名称,规格,单位,分类,当前库存,预警值,低库存");
        builder.append("\r\n");

        for (StockVO item : records) {
            builder.append(toCsvValue(item.getProductId())).append(',')
                    .append(toCsvValue(item.getProductCode())).append(',')
                    .append(toCsvValue(item.getProductName())).append(',')
                    .append(toCsvValue(item.getSpecification())).append(',')
                    .append(toCsvValue(item.getUnit())).append(',')
                    .append(toCsvValue(item.getCategory())).append(',')
                    .append(toCsvValue(item.getQuantity())).append(',')
                    .append(toCsvValue(item.getWarningQuantity())).append(',')
                    .append(toCsvValue(resolveLowStockText(item.getLowStock())))
                    .append("\r\n");
        }

        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String resolveLowStockText(Integer lowStock) {
        return Integer.valueOf(1).equals(lowStock) ? "是" : "否";
    }

    private String defaultText(String value) {
        return value == null ? "" : value.trim();
    }

    private String toCsvValue(Object value) {
        if (value == null) {
            return "\"\"";
        }

        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private String resolveAdjustReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return "手工修改库存";
        }
        return reason.trim();
    }
}
