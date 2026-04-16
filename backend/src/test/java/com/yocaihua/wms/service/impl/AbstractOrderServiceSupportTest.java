package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.CurrentUserContext;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbstractOrderServiceSupportTest {

    private final TestOrderServiceSupport support = new TestOrderServiceSupport();

    @BeforeEach
    void setUp() {
        CurrentUserContext.clear();
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void ensureAdminPermission_shouldThrow_whenCurrentUserIsNotAdmin() {
        CurrentUserContext.setRole("OPERATOR");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> support.callEnsureAdminPermission("确认入库"));

        assertEquals("仅管理员可执行：确认入库", exception.getMessage());
    }

    @Test
    void ensureAdminPermission_shouldPass_whenCurrentUserIsAdmin() {
        CurrentUserContext.setRole("ADMIN");

        assertDoesNotThrow(() -> support.callEnsureAdminPermission("确认入库"));
    }

    @Test
    void normalizeVoidReason_shouldThrow_whenReasonIsNullOrBlank() {
        BusinessException nullException = assertThrows(BusinessException.class,
                () -> support.callNormalizeVoidReason(null));
        BusinessException blankException = assertThrows(BusinessException.class,
                () -> support.callNormalizeVoidReason("   "));

        assertEquals("作废原因不能为空", nullException.getMessage());
        assertEquals("作废原因不能为空", blankException.getMessage());
    }

    @Test
    void normalizeVoidReason_shouldTrimReason_whenReasonIsPresent() {
        String result = support.callNormalizeVoidReason("  录入错误  ");

        assertEquals("录入错误", result);
    }

    @Test
    void buildVoidRemark_shouldReturnVoidReasonOnly_whenOriginalRemarkIsBlank() {
        String result = support.callBuildVoidRemark("   ", "录入错误");

        assertEquals("作废原因：录入错误", result);
    }

    @Test
    void buildVoidRemark_shouldAppendOriginalRemark_whenOriginalRemarkIsPresent() {
        String result = support.callBuildVoidRemark("  原备注  ", "录入错误");

        assertEquals("原单备注：原备注；作废原因：录入错误", result);
    }

    @Test
    void normalizePageNum_shouldReturnOne_whenPageNumIsNullOrLessThanOne() {
        assertEquals(1, support.callNormalizePageNum(null));
        assertEquals(1, support.callNormalizePageNum(0));
        assertEquals(1, support.callNormalizePageNum(-1));
        assertEquals(3, support.callNormalizePageNum(3));
    }

    @Test
    void normalizePageSize_shouldReturnDefaultOrCappedValue() {
        assertEquals(10, support.callNormalizePageSize(null));
        assertEquals(10, support.callNormalizePageSize(0));
        assertEquals(10, support.callNormalizePageSize(-5));
        assertEquals(50, support.callNormalizePageSize(50));
        assertEquals(200, support.callNormalizePageSize(201));
    }

    @Test
    void defaultText_shouldReturnDashForNullOrBlank_andTrimNonBlankText() {
        assertEquals("-", support.callDefaultText(null));
        assertEquals("-", support.callDefaultText("   "));
        assertEquals("测试文本", support.callDefaultText("  测试文本  "));
    }

    @Test
    void formatAmount_shouldReturnDashForNull_andStripTrailingZeros() {
        assertEquals("-", support.callFormatAmount(null));
        assertEquals("10", support.callFormatAmount(new BigDecimal("10.00")));
        assertEquals("10.5", support.callFormatAmount(new BigDecimal("10.50")));
    }

    @Test
    void formatTime_shouldReturnDashForNull_andFormatDateTime() {
        LocalDateTime value = LocalDateTime.of(2026, 4, 14, 9, 30, 15);

        assertEquals("-", support.callFormatTime(null));
        assertEquals("2026-04-14 09:30:15", support.callFormatTime(value));
    }

    @Test
    void writeSummaryRow_shouldWriteCellsAndReturnNextRowIndex() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("summary");

            int nextRowIndex = support.callWriteSummaryRow(sheet, 2, "单据编号", "  IN20260414  ", "备注", "   ");

            Row row = sheet.getRow(2);
            assertEquals(3, nextRowIndex);
            assertEquals("单据编号", row.getCell(0).getStringCellValue());
            assertEquals("IN20260414", row.getCell(1).getStringCellValue());
            assertEquals("备注", row.getCell(3).getStringCellValue());
            assertEquals("-", row.getCell(4).getStringCellValue());
        }
    }

    @Test
    void compileReport_shouldReturnCompiledReport_whenTemplateExists() {
        JasperReport report = support.callCompileReport("reports/inbound-order-export.jrxml");

        assertNotNull(report);
    }

    @Test
    void compileReport_shouldThrowBusinessException_whenTemplateDoesNotExist() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> support.callCompileReport("reports/not-exists.jrxml"));

        assertEquals("加载报表模板失败", exception.getMessage());
    }

    private static final class TestOrderServiceSupport extends AbstractOrderServiceSupport {
        void callEnsureAdminPermission(String action) {
            ensureAdminPermission(action);
        }

        String callNormalizeVoidReason(String voidReason) {
            return normalizeVoidReason(voidReason);
        }

        String callBuildVoidRemark(String orderRemark, String voidReason) {
            return buildVoidRemark(orderRemark, voidReason);
        }

        int callNormalizePageNum(Integer pageNum) {
            return normalizePageNum(pageNum);
        }

        int callNormalizePageSize(Integer pageSize) {
            return normalizePageSize(pageSize);
        }

        String callDefaultText(String value) {
            return defaultText(value);
        }

        String callFormatAmount(BigDecimal value) {
            return formatAmount(value);
        }

        String callFormatTime(LocalDateTime value) {
            return formatTime(value);
        }

        int callWriteSummaryRow(Sheet sheet, int rowIndex, String leftLabel, String leftValue, String rightLabel, String rightValue) {
            return writeSummaryRow(sheet, rowIndex, leftLabel, leftValue, rightLabel, rightValue);
        }

        JasperReport callCompileReport(String templatePath) {
            return compileReport(templatePath);
        }
    }
}
