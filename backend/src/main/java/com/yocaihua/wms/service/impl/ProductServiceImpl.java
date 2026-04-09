package com.yocaihua.wms.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Service
public class ProductServiceImpl implements ProductService {

    private static final int DEFAULT_INIT_STOCK = 0;
    private static final int DEFAULT_WARNING_QUANTITY = 10;
    private static final int MAX_PAGE_SIZE = 200;
    private static final int MAX_CUSTOM_FIELDS_JSON_LENGTH = 4000;
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    public PageResult<Product> getProductPage(String productCode, String productName, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        } else if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }

        int offset = (pageNum - 1) * pageSize;

        Long total = productMapper.count(productCode, productName);
        List<Product> list = productMapper.selectPage(productCode, productName, offset, pageSize);

        return new PageResult<>(total, pageNum, pageSize, list);
    }

    @Override
    public byte[] exportProductExcel(String productCode, String productName) {
        List<Product> records = productMapper.selectExportList(productCode, productName);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("商品列表");
            writeHeaderRow(sheet.createRow(0));

            int rowIndex = 1;
            for (Product product : records) {
                writeDataRow(sheet.createRow(rowIndex++), product);
            }

            for (int columnIndex = 0; columnIndex < 11; columnIndex++) {
                sheet.autoSizeColumn(columnIndex);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("导出商品列表失败");
        }
    }

    @Override
    @Transactional
    public String addProduct(ProductAddDTO productAddDTO) {
        productAddDTO.setCustomFieldsJson(validateAndNormalizeCustomFieldsJson(productAddDTO.getCustomFieldsJson()));
        productAddDTO.setStatus(normalizeStatus(productAddDTO.getStatus(), 1));

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
            stockMapper.deleteByProductId(id);
            return "删除商品成功";
        }

        throw new BusinessException("删除商品失败");
    }

    @Override
    public String updateProduct(ProductUpdateDTO productUpdateDTO) {
        productUpdateDTO.setCustomFieldsJson(validateAndNormalizeCustomFieldsJson(productUpdateDTO.getCustomFieldsJson()));

        Product existingProduct = productMapper.selectById(productUpdateDTO.getId());
        if (existingProduct == null) {
            throw new BusinessException("商品不存在");
        }
        Product duplicatedProduct = productMapper.selectByProductCode(productUpdateDTO.getProductCode());
        if (duplicatedProduct != null && !duplicatedProduct.getId().equals(productUpdateDTO.getId())) {
            throw new BusinessException("商品编码已存在");
        }
        productUpdateDTO.setStatus(normalizeStatus(productUpdateDTO.getStatus(), existingProduct.getStatus()));

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

    private void writeHeaderRow(Row row) {
        row.createCell(0).setCellValue("ID");
        row.createCell(1).setCellValue("商品编码");
        row.createCell(2).setCellValue("商品名称");
        row.createCell(3).setCellValue("规格");
        row.createCell(4).setCellValue("单位");
        row.createCell(5).setCellValue("分类");
        row.createCell(6).setCellValue("销售价");
        row.createCell(7).setCellValue("状态");
        row.createCell(8).setCellValue("备注");
        row.createCell(9).setCellValue("创建时间");
        row.createCell(10).setCellValue("更新时间");
    }

    private void writeDataRow(Row row, Product product) {
        row.createCell(0).setCellValue(product.getId() == null ? "" : String.valueOf(product.getId()));
        row.createCell(1).setCellValue(defaultText(product.getProductCode()));
        row.createCell(2).setCellValue(defaultText(product.getProductName()));
        row.createCell(3).setCellValue(defaultText(product.getSpecification()));
        row.createCell(4).setCellValue(defaultText(product.getUnit()));
        row.createCell(5).setCellValue(defaultText(product.getCategory()));
        row.createCell(6).setCellValue(product.getSalePrice() == null ? "" : product.getSalePrice().stripTrailingZeros().toPlainString());
        row.createCell(7).setCellValue(resolveStatusText(product.getStatus()));
        row.createCell(8).setCellValue(defaultText(product.getRemark()));
        row.createCell(9).setCellValue(formatTime(product.getCreatedTime()));
        row.createCell(10).setCellValue(formatTime(product.getUpdatedTime()));
    }

    private String resolveStatusText(Integer status) {
        return Integer.valueOf(1).equals(status) ? "启用" : "停用";
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "" : EXPORT_TIME_FORMATTER.format(value);
    }

    private String defaultText(String value) {
        return value == null ? "" : value.trim();
    }

    private String validateAndNormalizeCustomFieldsJson(String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() > MAX_CUSTOM_FIELDS_JSON_LENGTH) {
            throw new BusinessException("自定义字段长度不能超过4000个字符");
        }
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(value);
            if (jsonNode == null || !jsonNode.isObject()) {
                throw new BusinessException("自定义字段必须是JSON对象格式");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("自定义字段不是合法JSON，请检查格式");
        }
        return value;
    }

    private Integer normalizeStatus(Integer status, Integer defaultValue) {
        if (status == null) {
            return defaultValue;
        }
        if (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status)) {
            throw new BusinessException("商品状态只能为启用或停用");
        }
        return status;
    }
}
