package com.yocaihua.wms.dto;


import lombok.Data;

@Data
public class PythonOcrRecognizeResponseDTO {
    private Boolean success;
    private String message;
    private PythonOcrRecognizeDataDTO data;
}