package com.yocaihua.wms.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.dto.CustomerAddDTO;
import com.yocaihua.wms.dto.CustomerUpdateDTO;
import com.yocaihua.wms.entity.Customer;
import com.yocaihua.wms.mapper.CustomerMapper;
import com.yocaihua.wms.mapper.OutboundOrderMapper;
import com.yocaihua.wms.service.CustomerService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final int MAX_PAGE_SIZE = 200;
    private static final int MAX_CUSTOM_FIELDS_JSON_LENGTH = 4000;
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CustomerMapper customerMapper;
    private final OutboundOrderMapper outboundOrderMapper;

    public CustomerServiceImpl(CustomerMapper customerMapper,
                               OutboundOrderMapper outboundOrderMapper) {
        this.customerMapper = customerMapper;
        this.outboundOrderMapper = outboundOrderMapper;
    }

    @Override
    public PageResult<Customer> getCustomerPage(String customerCode, String customerName, Integer pageNum, Integer pageSize) {
        String normalizedCustomerCode = normalizeQueryKeyword(customerCode);
        String normalizedCustomerName = normalizeQueryKeyword(customerName);

        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        } else if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }

        int offset = (pageNum - 1) * pageSize;

        Long total = customerMapper.count(normalizedCustomerCode, normalizedCustomerName);
        List<Customer> list = customerMapper.selectPage(normalizedCustomerCode, normalizedCustomerName, offset, pageSize);

        if (total != null && total > 0 && (list == null || list.isEmpty())) {
            List<Customer> fallbackAll = customerMapper.selectPage(null, null, 0, MAX_PAGE_SIZE);
            if (fallbackAll != null && !fallbackAll.isEmpty()) {
                List<Customer> filtered = fallbackAll.stream()
                        .filter(c -> matchKeyword(c.getCustomerCode(), normalizedCustomerCode))
                        .filter(c -> matchKeyword(c.getCustomerName(), normalizedCustomerName))
                        .collect(Collectors.toList());
                total = (long) filtered.size();
                int fromIndex = Math.min(offset, filtered.size());
                int toIndex = Math.min(fromIndex + pageSize, filtered.size());
                list = filtered.subList(fromIndex, toIndex);
            } else {
                total = 0L;
                list = Collections.emptyList();
            }
        }

        return new PageResult<>(total, pageNum, pageSize, list);
    }

    @Override
    public byte[] exportCustomerExcel(String customerCode, String customerName) {
        String normalizedCustomerCode = normalizeQueryKeyword(customerCode);
        String normalizedCustomerName = normalizeQueryKeyword(customerName);
        List<Customer> records = customerMapper.selectExportList(normalizedCustomerCode, normalizedCustomerName);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("客户列表");
            writeHeaderRow(sheet.createRow(0));

            int rowIndex = 1;
            for (Customer customer : records) {
                writeDataRow(sheet.createRow(rowIndex++), customer);
            }

            for (int columnIndex = 0; columnIndex < 10; columnIndex++) {
                sheet.autoSizeColumn(columnIndex);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("导出客户列表失败");
        }
    }

    private String normalizeQueryKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean matchKeyword(String source, String keyword) {
        if (keyword == null) {
            return true;
        }
        if (source == null) {
            return false;
        }
        return source.contains(keyword);
    }

    @Override
    public Customer getCustomerById(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }
        return customer;
    }

    @Override
    public String addCustomer(CustomerAddDTO customerAddDTO) {
        customerAddDTO.setCustomFieldsJson(validateAndNormalizeCustomFieldsJson(customerAddDTO.getCustomFieldsJson()));
        customerAddDTO.setStatus(normalizeStatus(customerAddDTO.getStatus(), 1));
        Customer existingCustomer = customerMapper.selectByCustomerCode(customerAddDTO.getCustomerCode());
        if (existingCustomer != null) {
            throw new BusinessException("客户编码已存在");
        }

        int rows = customerMapper.insert(customerAddDTO);
        if (rows > 0) {
            return "新增客户成功";
        }
        throw new BusinessException("新增客户失败");
    }

    @Override
    public String updateCustomer(CustomerUpdateDTO customerUpdateDTO) {
        customerUpdateDTO.setCustomFieldsJson(validateAndNormalizeCustomFieldsJson(customerUpdateDTO.getCustomFieldsJson()));
        Customer existingCustomer = customerMapper.selectById(customerUpdateDTO.getId());
        if (existingCustomer == null) {
            throw new BusinessException("客户不存在");
        }
        Customer duplicatedCustomer = customerMapper.selectByCustomerCode(customerUpdateDTO.getCustomerCode());
        if (duplicatedCustomer != null && !duplicatedCustomer.getId().equals(customerUpdateDTO.getId())) {
            throw new BusinessException("客户编码已存在");
        }
        customerUpdateDTO.setStatus(normalizeStatus(customerUpdateDTO.getStatus(), existingCustomer.getStatus()));

        int rows = customerMapper.updateById(customerUpdateDTO);
        if (rows > 0) {
            return "修改客户成功";
        }
        throw new BusinessException("修改客户失败");
    }

    @Override
    public String deleteCustomer(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }

        int outboundRefCount = outboundOrderMapper.countByCustomerId(id);
        int legacyNameRefCount = outboundOrderMapper.countByCustomerNameWhenCustomerIdMissing(customer.getCustomerName());
        if (outboundRefCount > 0 || legacyNameRefCount > 0) {
            throw new BusinessException("客户已被出库单引用，不能删除");
        }

        int rows = customerMapper.deleteById(id);
        if (rows > 0) {
            return "删除客户成功";
        }

        throw new BusinessException("删除客户失败");
    }

    private void writeHeaderRow(Row row) {
        row.createCell(0).setCellValue("ID");
        row.createCell(1).setCellValue("客户编码");
        row.createCell(2).setCellValue("客户名称");
        row.createCell(3).setCellValue("联系人");
        row.createCell(4).setCellValue("联系电话");
        row.createCell(5).setCellValue("地址");
        row.createCell(6).setCellValue("备注");
        row.createCell(7).setCellValue("状态");
        row.createCell(8).setCellValue("创建时间");
        row.createCell(9).setCellValue("更新时间");
    }

    private void writeDataRow(Row row, Customer customer) {
        row.createCell(0).setCellValue(customer.getId() == null ? "" : String.valueOf(customer.getId()));
        row.createCell(1).setCellValue(defaultText(customer.getCustomerCode()));
        row.createCell(2).setCellValue(defaultText(customer.getCustomerName()));
        row.createCell(3).setCellValue(defaultText(customer.getContactPerson()));
        row.createCell(4).setCellValue(defaultText(customer.getPhone()));
        row.createCell(5).setCellValue(defaultText(customer.getAddress()));
        row.createCell(6).setCellValue(defaultText(customer.getRemark()));
        row.createCell(7).setCellValue(resolveStatusText(customer.getStatus()));
        row.createCell(8).setCellValue(formatTime(customer.getCreatedTime()));
        row.createCell(9).setCellValue(formatTime(customer.getUpdatedTime()));
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
            throw new BusinessException("客户自定义字段长度不能超过4000个字符");
        }
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(value);
            if (jsonNode == null || !jsonNode.isObject()) {
                throw new BusinessException("客户自定义字段必须是JSON对象格式");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("客户自定义字段不是合法JSON，请检查格式");
        }
        return value;
    }

    private Integer normalizeStatus(Integer status, Integer defaultValue) {
        if (status == null) {
            return defaultValue;
        }
        if (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status)) {
            throw new BusinessException("客户状态只能为启用或停用");
        }
        return status;
    }
}
