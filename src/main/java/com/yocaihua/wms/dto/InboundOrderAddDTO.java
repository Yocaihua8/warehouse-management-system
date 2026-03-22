package com.yocaihua.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class InboundOrderAddDTO {

    @NotBlank(message = "供应商名称不能为空")
    private String supplierName;

    private String remark;

    @NotEmpty(message = "入库明细不能为空")
    @Valid
    private List<InboundOrderItemAddDTO> itemList;
}