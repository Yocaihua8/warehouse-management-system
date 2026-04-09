package com.yocaihua.wms.dto;

import lombok.Data;

import java.util.List;

@Data
public class PythonOcrRecognizeDataDTO {
    private String supplierName;
    private String customerName;
    private String rawText;
    private List<String> warnings;
    private List<PythonOcrItemDTO> items;
}
