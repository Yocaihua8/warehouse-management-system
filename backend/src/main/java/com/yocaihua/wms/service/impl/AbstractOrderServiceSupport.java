package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.CurrentUserContext;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

abstract class AbstractOrderServiceSupport {
    protected static final int MAX_PAGE_SIZE = 200;
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected void ensureAdminPermission(String action) {
        if (!CurrentUserContext.isAdmin()) {
            throw new BusinessException("仅管理员可执行：" + action);
        }
    }

    protected String normalizeVoidReason(String voidReason) {
        if (voidReason == null || voidReason.trim().isEmpty()) {
            throw new BusinessException("作废原因不能为空");
        }
        return voidReason.trim();
    }

    protected String buildVoidRemark(String orderRemark, String voidReason) {
        if (orderRemark == null || orderRemark.trim().isEmpty()) {
            return "作废原因：" + voidReason;
        }
        return "原单备注：" + orderRemark.trim() + "；作废原因：" + voidReason;
    }

    protected int normalizePageNum(Integer pageNum) {
        if (pageNum == null || pageNum < 1) {
            return 1;
        }
        return pageNum;
    }

    protected int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    protected JasperReport compileReport(String templatePath) {
        try (InputStream inputStream = new ClassPathResource(templatePath).getInputStream()) {
            return JasperCompileManager.compileReport(inputStream);
        } catch (IOException | JRException ex) {
            throw new BusinessException("加载报表模板失败");
        }
    }

    protected int writeSummaryRow(Sheet sheet, int rowIndex, String leftLabel, String leftValue, String rightLabel, String rightValue) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(leftLabel);
        row.createCell(1).setCellValue(defaultText(leftValue));
        row.createCell(3).setCellValue(rightLabel);
        row.createCell(4).setCellValue(defaultText(rightValue));
        return rowIndex + 1;
    }

    protected String formatTime(LocalDateTime value) {
        return value == null ? "-" : EXPORT_TIME_FORMATTER.format(value);
    }

    protected String formatAmount(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    protected String defaultText(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }
}
