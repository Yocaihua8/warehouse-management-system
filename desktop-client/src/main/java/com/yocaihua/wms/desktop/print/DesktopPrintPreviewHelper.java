package com.yocaihua.wms.desktop.print;

import com.yocaihua.wms.desktop.module.inbound.InboundOrderDetailData;
import com.yocaihua.wms.desktop.module.inbound.InboundOrderItemDetailRow;
import com.yocaihua.wms.desktop.module.outbound.OutboundOrderDetailData;
import com.yocaihua.wms.desktop.module.outbound.OutboundOrderItemDetailRow;

import java.awt.Desktop;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class DesktopPrintPreviewHelper {

    private static final int PRINT_MIN_ROWS = 12;

    private DesktopPrintPreviewHelper() {
    }

    public static Path exportInboundPreview(InboundOrderDetailData detail) throws IOException {
        return writeHtml("inbound", detail == null ? null : detail.getOrderNo(), buildInboundHtml(detail));
    }

    public static Path exportOutboundPreview(OutboundOrderDetailData detail) throws IOException {
        return writeHtml("outbound", detail == null ? null : detail.getOrderNo(), buildOutboundHtml(detail));
    }

    public static boolean openPreview(Path filePath) throws IOException {
        if (filePath == null) {
            throw new IOException("打印预览文件不存在。");
        }

        if (!Desktop.isDesktopSupported()) {
            return false;
        }

        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
            desktop.browse(filePath.toUri());
            return true;
        }
        if (desktop.isSupported(Desktop.Action.OPEN)) {
            desktop.open(filePath.toFile());
            return true;
        }
        return false;
    }

    private static Path writeHtml(String prefix, String orderNo, String html) throws IOException {
        Path previewDir = Paths.get(System.getProperty("java.io.tmpdir"), "wms-desktop-print");
        Files.createDirectories(previewDir);

        String fileName = prefix + "-" + sanitizeFileName(defaultText(orderNo, "preview")) + "-" + System.currentTimeMillis() + ".html";
        Path filePath = previewDir.resolve(fileName);
        Files.writeString(filePath, html, StandardCharsets.UTF_8);
        return filePath;
    }

    private static String buildInboundHtml(InboundOrderDetailData detail) {
        InboundOrderDetailData safeDetail = detail == null ? new InboundOrderDetailData() : detail;
        String aiExtraRow = "";
        if ("AI".equalsIgnoreCase(safeDetail.getSourceType())) {
            aiExtraRow = """
                    <tr>
                      <td colspan="6">AI任务号：%s</td>
                      <td colspan="3">源文件名</td>
                      <td colspan="2">%s</td>
                    </tr>
                    """.formatted(
                    escapeHtml(defaultText(safeDetail.getAiTaskNo(), "-")),
                    escapeHtml(defaultText(safeDetail.getAiSourceFileName(), "-"))
            );
        }

        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <title>采购入库单打印预览</title>
                  <style>
                    body { font-family: "Microsoft YaHei", sans-serif; margin: 0; background: #fff; color: #000; }
                    .print-page { width: 1000px; margin: 0 auto; padding: 12px 8px; }
                    .print-title { text-align: center; font-size: 34px; font-weight: 700; margin-bottom: 10px; }
                    .print-table { width: 100%%; border-collapse: collapse; font-size: 20px; }
                    .print-table td, .print-table th { border: 1px solid #111; padding: 4px 6px; line-height: 1.35; }
                    .print-table .detail-row td { height: 52px; min-height: 52px; padding-top: 0; padding-bottom: 0; vertical-align: middle; line-height: 52px; white-space: nowrap; }
                    .print-signature { margin-top: 10px; display: flex; justify-content: space-between; font-size: 18px; }
                    @media print { .print-page { width: auto; margin: 0; padding: 0; } }
                  </style>
                </head>
                <body>
                <div class="print-page">
                  <div class="print-title">采购入库单</div>
                  <table class="print-table">
                    <tr>
                      <td colspan="6">入库仓库：总仓库</td>
                      <td colspan="3">单据编号</td>
                      <td colspan="2">%s</td>
                    </tr>
                    <tr>
                      <td colspan="6">供货单位：%s</td>
                      <td colspan="3">录单日期</td>
                      <td colspan="2">%s</td>
                    </tr>
                    <tr>
                      <td colspan="6">来源类型：%s</td>
                      <td colspan="3">AI记录ID</td>
                      <td colspan="2">%s</td>
                    </tr>
                    %s
                    <tr>
                      <td colspan="11">备注摘要：%s</td>
                    </tr>
                    <tr>
                      <th>序号</th>
                      <th colspan="2">商品全名</th>
                      <th>规格</th>
                      <th>单位</th>
                      <th>数量</th>
                      <th>单价</th>
                      <th colspan="2">金额</th>
                      <th colspan="2">备注</th>
                    </tr>
                    %s
                    <tr>
                      <td colspan="6">合计</td>
                      <td colspan="2">%s</td>
                      <td colspan="3"></td>
                    </tr>
                    <tr>
                      <td colspan="11">金额合计（大写）：%s</td>
                    </tr>
                    <tr>
                      <td colspan="6">公司名称：仓库管理系统</td>
                      <td colspan="3">公司电话</td>
                      <td colspan="2"></td>
                    </tr>
                    <tr>
                      <td colspan="6">公司地址：-</td>
                      <td colspan="3">联系人</td>
                      <td colspan="2"></td>
                    </tr>
                  </table>
                  <div class="print-signature">
                    <span>经手人：</span>
                    <span>录单人：</span>
                    <span>审核人：</span>
                    <span>库管（签字）：</span>
                    <span>出纳（签字）：</span>
                  </div>
                </div>
                <script>
                  window.onload = function () {
                    setTimeout(function () {
                      window.print();
                    }, 200);
                  };
                </script>
                </body>
                </html>
                """.formatted(
                escapeHtml(defaultText(safeDetail.getOrderNo(), "-")),
                escapeHtml(defaultText(safeDetail.getSupplierName(), "-")),
                escapeHtml(defaultText(safeDetail.getCreatedTime(), "-")),
                escapeHtml(defaultText(safeDetail.getSourceTypeText(), "手工创建")),
                escapeHtml("AI".equalsIgnoreCase(safeDetail.getSourceType())
                        ? defaultText(safeDetail.getAiRecordId() == null ? null : String.valueOf(safeDetail.getAiRecordId()), "-")
                        : "-"),
                aiExtraRow,
                escapeHtml(defaultText(safeDetail.getRemark(), "-")),
                buildInboundRows(safeDetail.getItemList()),
                escapeHtml(defaultText(safeDetail.getTotalAmountText(), "-")),
                escapeHtml(toChineseAmount(safeDetail.getTotalAmount()))
        );
    }

    private static String buildOutboundHtml(OutboundOrderDetailData detail) {
        OutboundOrderDetailData safeDetail = detail == null ? new OutboundOrderDetailData() : detail;
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <title>销售出库单打印预览</title>
                  <style>
                    body { font-family: "Microsoft YaHei", sans-serif; margin: 0; background: #fff; color: #000; }
                    .print-page { width: 1000px; margin: 0 auto; padding: 12px 8px; }
                    .print-title { text-align: center; font-size: 34px; font-weight: 700; margin-bottom: 10px; }
                    .print-table { width: 100%%; border-collapse: collapse; font-size: 20px; }
                    .print-table td, .print-table th { border: 1px solid #111; padding: 4px 6px; line-height: 1.35; }
                    .print-table .detail-row td { height: 48px; min-height: 48px; padding-top: 0; padding-bottom: 0; vertical-align: middle; line-height: 48px; white-space: nowrap; }
                    .print-signature { margin-top: 10px; display: flex; justify-content: space-between; font-size: 18px; }
                    @media print { .print-page { width: auto; margin: 0; padding: 0; } }
                  </style>
                </head>
                <body>
                <div class="print-page">
                  <div class="print-title">销售出库单</div>
                  <table class="print-table">
                    <tr>
                      <td colspan="6">发货仓库：总仓库</td>
                      <td colspan="3">单据编号</td>
                      <td colspan="2">%s</td>
                    </tr>
                    <tr>
                      <td colspan="6">购货单位：%s</td>
                      <td colspan="3">录单日期</td>
                      <td colspan="2">%s</td>
                    </tr>
                    <tr>
                      <td colspan="11">备注摘要：%s</td>
                    </tr>
                    <tr>
                      <th>序号</th>
                      <th colspan="2">商品全名</th>
                      <th>规格</th>
                      <th>单位</th>
                      <th>数量</th>
                      <th>单价</th>
                      <th colspan="2">金额</th>
                      <th colspan="2">备注</th>
                    </tr>
                    %s
                    <tr>
                      <td colspan="6">合计</td>
                      <td colspan="2">%s</td>
                      <td colspan="3"></td>
                    </tr>
                    <tr>
                      <td colspan="11">金额合计（大写）：%s</td>
                    </tr>
                    <tr>
                      <td colspan="6">公司名称：仓库管理系统</td>
                      <td colspan="3">公司电话</td>
                      <td colspan="2"></td>
                    </tr>
                    <tr>
                      <td colspan="6">公司地址：-</td>
                      <td colspan="3">联系人</td>
                      <td colspan="2"></td>
                    </tr>
                  </table>
                  <div class="print-signature">
                    <span>经手人：</span>
                    <span>录单人：</span>
                    <span>审核人：</span>
                    <span>库管（签字）：</span>
                    <span>出纳（签字）：</span>
                  </div>
                </div>
                <script>
                  window.onload = function () {
                    setTimeout(function () {
                      window.print();
                    }, 200);
                  };
                </script>
                </body>
                </html>
                """.formatted(
                escapeHtml(defaultText(safeDetail.getOrderNo(), "-")),
                escapeHtml(defaultText(safeDetail.getCustomerName(), "-")),
                escapeHtml(defaultText(safeDetail.getCreatedTime(), "-")),
                escapeHtml(defaultText(safeDetail.getRemark(), "-")),
                buildOutboundRows(safeDetail.getItemList()),
                escapeHtml(defaultText(safeDetail.getTotalAmountText(), "-")),
                escapeHtml(toChineseAmount(safeDetail.getTotalAmount()))
        );
    }

    private static String buildInboundRows(List<InboundOrderItemDetailRow> items) {
        StringBuilder builder = new StringBuilder();
        int rowCount = Math.max(PRINT_MIN_ROWS, items == null ? 0 : items.size());
        for (int i = 0; i < rowCount; i++) {
            if (items != null && i < items.size()) {
                InboundOrderItemDetailRow item = items.get(i);
                builder.append("""
                        <tr class="detail-row">
                          <td>%s</td>
                          <td colspan="2">%s</td>
                          <td>%s</td>
                          <td>%s</td>
                          <td>%s</td>
                          <td>%s</td>
                          <td colspan="2">%s</td>
                          <td colspan="2">%s</td>
                        </tr>
                        """.formatted(
                        i + 1,
                        printCell(item.getProductName()),
                        printCell(item.getSpecification()),
                        printCell(item.getUnit()),
                        printCell(item.getQuantity()),
                        printCell(item.getUnitPriceText()),
                        printCell(item.getAmountText()),
                        printCell(item.getRemark())
                ));
            } else {
                builder.append(blankRow());
            }
        }
        return builder.toString();
    }

    private static String buildOutboundRows(List<OutboundOrderItemDetailRow> items) {
        StringBuilder builder = new StringBuilder();
        int rowCount = Math.max(PRINT_MIN_ROWS, items == null ? 0 : items.size());
        for (int i = 0; i < rowCount; i++) {
            if (items != null && i < items.size()) {
                OutboundOrderItemDetailRow item = items.get(i);
                builder.append("""
                        <tr class="detail-row">
                          <td>%s</td>
                          <td colspan="2">%s</td>
                          <td>%s</td>
                          <td>%s</td>
                          <td>%s</td>
                          <td>%s</td>
                          <td colspan="2">%s</td>
                          <td colspan="2">%s</td>
                        </tr>
                        """.formatted(
                        i + 1,
                        printCell(item.getProductName()),
                        printCell(item.getSpecification()),
                        printCell(item.getUnit()),
                        printCell(item.getQuantity()),
                        printCell(item.getUnitPriceText()),
                        printCell(item.getAmountText()),
                        printCell(item.getRemark())
                ));
            } else {
                builder.append(blankRow());
            }
        }
        return builder.toString();
    }

    private static String blankRow() {
        return """
                <tr class="detail-row">
                  <td>&nbsp;</td>
                  <td colspan="2">&nbsp;</td>
                  <td>&nbsp;</td>
                  <td>&nbsp;</td>
                  <td>&nbsp;</td>
                  <td>&nbsp;</td>
                  <td colspan="2">&nbsp;</td>
                  <td colspan="2">&nbsp;</td>
                </tr>
                """;
    }

    private static String printCell(Object value) {
        if (value == null) {
            return "&nbsp;";
        }
        String text = value instanceof BigDecimal
                ? ((BigDecimal) value).stripTrailingZeros().toPlainString()
                : String.valueOf(value).trim();
        return text.isEmpty() ? "&nbsp;" : escapeHtml(text);
    }

    private static String sanitizeFileName(String value) {
        return defaultText(value, "preview").replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String defaultText(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private static String toChineseAmount(BigDecimal value) {
        if (value == null) {
            return "-";
        }

        BigDecimal safeAmount = value.setScale(2, RoundingMode.HALF_UP);
        if (safeAmount.compareTo(BigDecimal.ZERO) == 0) {
            return "零元整";
        }

        String[] digitMap = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
        String[] smallUnitMap = {"", "拾", "佰", "仟"};
        String[] sectionUnitMap = {"", "万", "亿", "兆"};

        long integerPart = safeAmount.longValue();
        int fractionPart = safeAmount.remainder(BigDecimal.ONE).movePointRight(2).intValue();

        StringBuilder integerText = new StringBuilder();
        int sectionIndex = 0;
        boolean needZero = false;

        while (integerPart > 0) {
            int sectionNum = (int) (integerPart % 10000);
            if (sectionNum == 0) {
                if (integerText.length() > 0 && integerText.charAt(0) != '零') {
                    needZero = true;
                }
            } else {
                String sectionText = convertSection(sectionNum, digitMap, smallUnitMap);
                if (needZero) {
                    sectionText = "零" + sectionText;
                    needZero = false;
                }
                integerText.insert(0, sectionText + sectionUnitMap[sectionIndex]);
                if (sectionNum < 1000) {
                    needZero = true;
                }
            }
            sectionIndex++;
            integerPart = integerPart / 10000;
        }

        String result = normalizeChineseZeros(integerText.length() == 0 ? "零" : integerText.toString()) + "元";

        if (fractionPart == 0) {
            return result + "整";
        }

        int jiao = fractionPart / 10;
        int fen = fractionPart % 10;
        StringBuilder fractionText = new StringBuilder();
        if (jiao > 0) {
            fractionText.append(digitMap[jiao]).append("角");
        }
        if (fen > 0) {
            if (jiao == 0) {
                fractionText.append("零");
            }
            fractionText.append(digitMap[fen]).append("分");
        }
        return result + fractionText;
    }

    private static String convertSection(int sectionNum, String[] digitMap, String[] smallUnitMap) {
        StringBuilder sectionText = new StringBuilder();
        boolean zeroFlag = true;
        int unitPos = 0;
        int num = sectionNum;

        while (num > 0) {
            int digit = num % 10;
            if (digit == 0) {
                if (!zeroFlag) {
                    zeroFlag = true;
                    sectionText.insert(0, digitMap[0]);
                }
            } else {
                zeroFlag = false;
                sectionText.insert(0, digitMap[digit] + smallUnitMap[unitPos]);
            }
            unitPos++;
            num = num / 10;
        }
        return normalizeChineseZeros(sectionText.toString());
    }

    private static String normalizeChineseZeros(String value) {
        return value.replaceAll("零+", "零").replaceAll("零$", "");
    }
}
