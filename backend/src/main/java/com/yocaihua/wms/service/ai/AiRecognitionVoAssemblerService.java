package com.yocaihua.wms.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yocaihua.wms.dto.AiInboundConfirmItemDTO;
import com.yocaihua.wms.dto.AiOutboundConfirmItemDTO;
import com.yocaihua.wms.dto.PythonOcrItemDTO;
import com.yocaihua.wms.dto.PythonOcrRecognizeDataDTO;
import com.yocaihua.wms.entity.AiRecognitionRecord;
import com.yocaihua.wms.vo.AiInboundRecognizeItemVO;
import com.yocaihua.wms.vo.AiInboundRecognizeVO;
import com.yocaihua.wms.vo.AiOutboundRecognizeItemVO;
import com.yocaihua.wms.vo.AiOutboundRecognizeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiRecognitionVoAssemblerService {

    private static final String DOC_TYPE_INBOUND = "inbound";
    private static final String DOC_TYPE_OUTBOUND = "outbound";
    private static final String STATUS_WAIT_MANUAL_CONFIRM = "success";

    private final ObjectMapper objectMapper;
    private final AiOrderAssemblerService aiOrderAssemblerService;

    public AiInboundRecognizeVO buildInboundRecognizeResult(AiRecognitionRecord record,
                                                            String fileName,
                                                            String supplierName,
                                                            Long matchedSupplierId,
                                                            String supplierMatchStatus,
                                                            String rawText,
                                                            String warningsJson,
                                                            List<AiInboundRecognizeItemVO> itemVOList) {
        AiInboundRecognizeVO resultVO = new AiInboundRecognizeVO();
        resultVO.setRecordId(record.getId());
        resultVO.setTaskNo(record.getTaskNo());
        resultVO.setDocType(DOC_TYPE_INBOUND);
        resultVO.setRecognitionStatus(STATUS_WAIT_MANUAL_CONFIRM);
        resultVO.setSourceFileName(fileName);
        resultVO.setSupplierName(supplierName);
        resultVO.setMatchedSupplierId(matchedSupplierId);
        resultVO.setSupplierMatchStatus(supplierMatchStatus);
        resultVO.setRawText(rawText);
        resultVO.setWarningsJson(warningsJson);
        resultVO.setConfirmedOrderId(record.getConfirmedOrderId());
        resultVO.setItemList(itemVOList);
        return resultVO;
    }

    public AiOutboundRecognizeVO buildOutboundRecognizeResult(AiRecognitionRecord record,
                                                              String fileName,
                                                              String customerName,
                                                              Long matchedCustomerId,
                                                              String customerMatchStatus,
                                                              String rawText,
                                                              String warningsJson,
                                                              List<String> warnings,
                                                              List<AiOutboundRecognizeItemVO> itemVOList) {
        AiOutboundRecognizeVO resultVO = new AiOutboundRecognizeVO();
        resultVO.setRecordId(record.getId());
        resultVO.setTaskNo(record.getTaskNo());
        resultVO.setDocType(DOC_TYPE_OUTBOUND);
        resultVO.setRecognitionStatus(STATUS_WAIT_MANUAL_CONFIRM);
        resultVO.setSourceFileName(fileName);
        resultVO.setCustomerName(customerName);
        resultVO.setMatchedCustomerId(matchedCustomerId);
        resultVO.setCustomerMatchStatus(customerMatchStatus);
        resultVO.setRawText(rawText);
        resultVO.setWarningsJson(warningsJson);
        resultVO.setWarnings(warnings);
        resultVO.setConfirmedOrderId(record.getConfirmedOrderId());
        resultVO.setItemList(itemVOList);
        return resultVO;
    }

    public List<AiInboundRecognizeItemVO> buildEditedInboundItemVOList(List<AiInboundConfirmItemDTO> itemList) {
        List<AiInboundRecognizeItemVO> voList = new ArrayList<>();
        for (AiInboundConfirmItemDTO itemDTO : itemList) {
            AiInboundRecognizeItemVO vo = new AiInboundRecognizeItemVO();
            vo.setLineNo(itemDTO.getLineNo());
            vo.setProductName(normalizeOptionalText(itemDTO.getProductName()));
            vo.setSpecification(normalizeOptionalText(itemDTO.getSpecification()));
            vo.setUnit(normalizeOptionalText(itemDTO.getUnit()));
            vo.setQuantity(itemDTO.getQuantity());
            vo.setUnitPrice(itemDTO.getUnitPrice());
            vo.setAmount(aiOrderAssemblerService.resolveItemAmount(itemDTO.getAmount(), itemDTO.getQuantity(), itemDTO.getUnitPrice()));
            vo.setMatchedProductId(itemDTO.getMatchedProductId());
            vo.setMatchStatus(itemDTO.getMatchedProductId() != null ? "manual_confirmed" : "unmatched");
            vo.setRemark(normalizeOptionalText(itemDTO.getRemark()));
            voList.add(vo);
        }
        return voList;
    }

    public List<AiOutboundRecognizeItemVO> buildEditedOutboundItemVOList(List<AiOutboundConfirmItemDTO> itemList) {
        List<AiOutboundRecognizeItemVO> voList = new ArrayList<>();
        for (AiOutboundConfirmItemDTO itemDTO : itemList) {
            AiOutboundRecognizeItemVO vo = new AiOutboundRecognizeItemVO();
            vo.setLineNo(itemDTO.getLineNo());
            vo.setProductName(normalizeOptionalText(itemDTO.getProductName()));
            vo.setSpecification(normalizeOptionalText(itemDTO.getSpecification()));
            vo.setUnit(normalizeOptionalText(itemDTO.getUnit()));
            vo.setQuantity(itemDTO.getQuantity());
            vo.setUnitPrice(itemDTO.getUnitPrice());
            vo.setAmount(aiOrderAssemblerService.resolveItemAmount(itemDTO.getAmount(), itemDTO.getQuantity(), itemDTO.getUnitPrice()));
            vo.setMatchedProductId(itemDTO.getMatchedProductId());
            vo.setMatchStatus(itemDTO.getMatchedProductId() != null ? "manual_confirmed" : "unmatched");
            vo.setRemark(normalizeOptionalText(itemDTO.getRemark()));
            voList.add(vo);
        }
        return voList;
    }

    public String resolveRecognizedCustomerName(PythonOcrRecognizeDataDTO ocrData) {
        if (ocrData == null) {
            return "未识别客户";
        }
        String customerName = ocrData.getCustomerName();
        if (customerName == null || customerName.isBlank()) {
            customerName = ocrData.getSupplierName();
        }
        if (customerName == null || customerName.isBlank()) {
            return "未识别客户";
        }
        return customerName;
    }

    public List<AiInboundRecognizeItemVO> convertPythonItemsToInbound(List<PythonOcrItemDTO> pythonItems) {
        List<AiInboundRecognizeItemVO> list = new ArrayList<>();
        if (pythonItems == null) {
            return list;
        }

        for (PythonOcrItemDTO item : pythonItems) {
            AiInboundRecognizeItemVO vo = new AiInboundRecognizeItemVO();
            vo.setLineNo(item.getLineNo());
            vo.setProductName(item.getProductName());
            vo.setSpecification(item.getSpecification());
            vo.setUnit(item.getUnit());
            vo.setQuantity(item.getQuantity());
            vo.setUnitPrice(item.getUnitPrice());
            vo.setAmount(item.getAmount());
            vo.setMatchStatus("unmatched");
            vo.setRemark("python识别");
            list.add(vo);
        }
        return list;
    }

    public List<AiOutboundRecognizeItemVO> convertPythonItemsToOutbound(List<PythonOcrItemDTO> pythonItems) {
        List<AiOutboundRecognizeItemVO> list = new ArrayList<>();
        if (pythonItems == null) {
            return list;
        }

        for (PythonOcrItemDTO item : pythonItems) {
            AiOutboundRecognizeItemVO vo = new AiOutboundRecognizeItemVO();
            vo.setLineNo(item.getLineNo());
            vo.setProductName(item.getProductName());
            vo.setSpecification(item.getSpecification());
            vo.setUnit(item.getUnit());
            vo.setQuantity(item.getQuantity());
            vo.setUnitPrice(item.getUnitPrice());
            vo.setAmount(item.getAmount());
            vo.setMatchStatus("unmatched");
            vo.setRemark("python识别");
            list.add(vo);
        }
        return list;
    }

    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    private String normalizeOptionalText(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
