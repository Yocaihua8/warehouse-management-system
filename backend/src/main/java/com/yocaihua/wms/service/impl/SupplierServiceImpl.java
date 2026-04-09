package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.dto.SupplierAddDTO;
import com.yocaihua.wms.dto.SupplierUpdateDTO;
import com.yocaihua.wms.entity.Supplier;
import com.yocaihua.wms.mapper.InboundOrderMapper;
import com.yocaihua.wms.mapper.SupplierMapper;
import com.yocaihua.wms.service.SupplierService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SupplierServiceImpl implements SupplierService {

    private static final int MAX_PAGE_SIZE = 200;
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SupplierMapper supplierMapper;
    private final InboundOrderMapper inboundOrderMapper;

    public SupplierServiceImpl(SupplierMapper supplierMapper,
                               InboundOrderMapper inboundOrderMapper) {
        this.supplierMapper = supplierMapper;
        this.inboundOrderMapper = inboundOrderMapper;
    }

    @Override
    public PageResult<Supplier> getSupplierPage(String supplierCode, String supplierName, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        } else if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }

        int offset = (pageNum - 1) * pageSize;
        Long total = supplierMapper.count(supplierCode, supplierName);
        List<Supplier> list = supplierMapper.selectPage(supplierCode, supplierName, offset, pageSize);

        return new PageResult<>(total, pageNum, pageSize, list);
    }

    @Override
    public byte[] exportSupplierExcel(String supplierCode, String supplierName) {
        List<Supplier> records = supplierMapper.selectExportList(supplierCode, supplierName);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("供应商列表");
            writeHeaderRow(sheet.createRow(0));

            int rowIndex = 1;
            for (Supplier supplier : records) {
                writeDataRow(sheet.createRow(rowIndex++), supplier);
            }

            for (int columnIndex = 0; columnIndex < 10; columnIndex++) {
                sheet.autoSizeColumn(columnIndex);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("导出供应商列表失败");
        }
    }

    @Override
    public Supplier getSupplierById(Long id) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw new BusinessException("供应商不存在");
        }
        return supplier;
    }

    @Override
    public String addSupplier(SupplierAddDTO supplierAddDTO) {
        Supplier existingSupplier = supplierMapper.selectBySupplierCode(supplierAddDTO.getSupplierCode());
        if (existingSupplier != null) {
            throw new BusinessException("供应商编码已存在");
        }

        int rows = supplierMapper.insert(supplierAddDTO);
        if (rows <= 0) {
            throw new BusinessException("新增供应商失败");
        }

        return "新增供应商成功";
    }

    @Override
    public String updateSupplier(SupplierUpdateDTO supplierUpdateDTO) {
        Supplier existingSupplier = supplierMapper.selectById(supplierUpdateDTO.getId());
        if (existingSupplier == null) {
            throw new BusinessException("供应商不存在");
        }

        Supplier duplicatedSupplier = supplierMapper.selectBySupplierCodeExcludeId(
                supplierUpdateDTO.getSupplierCode(),
                supplierUpdateDTO.getId()
        );
        if (duplicatedSupplier != null) {
            throw new BusinessException("供应商编码已存在");
        }

        int rows = supplierMapper.updateById(supplierUpdateDTO);
        if (rows <= 0) {
            throw new BusinessException("修改供应商失败");
        }
        return "修改供应商成功";
    }

    @Override
    public String deleteSupplier(Long id) {
        Supplier existingSupplier = supplierMapper.selectById(id);
        if (existingSupplier == null) {
            throw new BusinessException("供应商不存在");
        }

        int inboundRefCount = inboundOrderMapper.countBySupplierId(id);
        int legacyNameRefCount = inboundOrderMapper.countBySupplierNameWhenSupplierIdMissing(existingSupplier.getSupplierName());
        if (inboundRefCount > 0 || legacyNameRefCount > 0) {
            throw new BusinessException("供应商已被入库单引用，不能删除");
        }

        int rows = supplierMapper.deleteById(id);
        if (rows <= 0) {
            throw new BusinessException("删除供应商失败");
        }
        return "删除供应商成功";
    }

    private void writeHeaderRow(Row row) {
        row.createCell(0).setCellValue("ID");
        row.createCell(1).setCellValue("供应商编码");
        row.createCell(2).setCellValue("供应商名称");
        row.createCell(3).setCellValue("联系人");
        row.createCell(4).setCellValue("联系电话");
        row.createCell(5).setCellValue("地址");
        row.createCell(6).setCellValue("备注");
        row.createCell(7).setCellValue("状态");
        row.createCell(8).setCellValue("创建时间");
        row.createCell(9).setCellValue("更新时间");
    }

    private void writeDataRow(Row row, Supplier supplier) {
        row.createCell(0).setCellValue(supplier.getId() == null ? "" : String.valueOf(supplier.getId()));
        row.createCell(1).setCellValue(defaultText(supplier.getSupplierCode()));
        row.createCell(2).setCellValue(defaultText(supplier.getSupplierName()));
        row.createCell(3).setCellValue(defaultText(supplier.getContactPerson()));
        row.createCell(4).setCellValue(defaultText(supplier.getPhone()));
        row.createCell(5).setCellValue(defaultText(supplier.getAddress()));
        row.createCell(6).setCellValue(defaultText(supplier.getRemark()));
        row.createCell(7).setCellValue(resolveStatusText(supplier.getStatus()));
        row.createCell(8).setCellValue(formatTime(supplier.getCreatedTime()));
        row.createCell(9).setCellValue(formatTime(supplier.getUpdatedTime()));
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
}
