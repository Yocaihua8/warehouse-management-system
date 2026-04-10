package com.yocaihua.wms.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.entity.AiRecognitionItem;
import com.yocaihua.wms.entity.AiRecognitionRecord;
import com.yocaihua.wms.mapper.AiRecognitionItemMapper;
import com.yocaihua.wms.mapper.AiRecognitionRecordMapper;
import com.yocaihua.wms.vo.AiInboundRecognizeItemVO;
import com.yocaihua.wms.vo.AiInboundRecognizeVO;
import com.yocaihua.wms.vo.AiOutboundRecognizeItemVO;
import com.yocaihua.wms.vo.AiOutboundRecognizeVO;
import com.yocaihua.wms.vo.AiRecognitionRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiRecognitionRecordService {

    private static final String STATUS_PENDING = "pending";
    private static final String DOC_TYPE_INBOUND = "inbound";
    private static final String STATUS_WAIT_MANUAL_CONFIRM = "success";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_CONFIRMED_TO_ORDER = "confirmed";

    private final AiRecognitionRecordMapper aiRecognitionRecordMapper;
    private final AiRecognitionItemMapper aiRecognitionItemMapper;
    private final ObjectMapper objectMapper;

    public AiRecognitionRecord createPendingRecord(String taskNo, String fileName, String operator, String docType) {
        AiRecognitionRecord record = new AiRecognitionRecord();
        record.setTaskNo(taskNo);
        record.setDocType(docType);
        record.setSourceFileName(fileName);
        record.setSourceFilePath(null);
        record.setRecognitionStatus(STATUS_PENDING);
        record.setCreatedBy(operator);
        aiRecognitionRecordMapper.insert(record);
        return record;
    }

    public AiRecognitionRecord getById(Long recordId) {
        return aiRecognitionRecordMapper.selectById(recordId);
    }

    public AiRecognitionRecord getByIdForUpdate(Long recordId) {
        return aiRecognitionRecordMapper.selectByIdForUpdate(recordId);
    }

    public List<AiRecognitionItem> getItemsByRecordId(Long recordId) {
        return aiRecognitionItemMapper.selectByRecordId(recordId);
    }

    public PageResult<AiRecognitionRecordVO> listInboundRecords(Integer pageNum, Integer pageSize) {
        int currentPage = normalizePageNum(pageNum);
        int currentSize = normalizePageSize(pageSize);
        int offset = (currentPage - 1) * currentSize;

        Long total = aiRecognitionRecordMapper.countInboundRecords();
        List<AiRecognitionRecordVO> list = aiRecognitionRecordMapper.selectInboundRecordPage(offset, currentSize);
        return new PageResult<>(total, currentPage, currentSize, list);
    }

    public PageResult<AiRecognitionRecordVO> listOutboundRecords(Integer pageNum, Integer pageSize) {
        int currentPage = normalizePageNum(pageNum);
        int currentSize = normalizePageSize(pageSize);
        int offset = (currentPage - 1) * currentSize;

        Long total = aiRecognitionRecordMapper.countOutboundRecords();
        List<AiRecognitionRecordVO> list = aiRecognitionRecordMapper.selectOutboundRecordPage(offset, currentSize);
        return new PageResult<>(total, currentPage, currentSize, list);
    }

    public AiInboundRecognizeVO getInboundRecordDetail(Long recordId) {
        if (recordId == null) {
            throw new BusinessException("记录ID不能为空");
        }

        AiRecognitionRecord record = aiRecognitionRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("AI识别记录不存在");
        }
        if (!DOC_TYPE_INBOUND.equals(record.getDocType())) {
            throw new BusinessException("该识别记录不是入库单类型");
        }

        List<AiInboundRecognizeItemVO> itemVOList = buildInboundItemVOList(aiRecognitionItemMapper.selectByRecordId(recordId));
        AiInboundRecognizeVO savedResult = readSavedInboundRecognizeResult(record);

        AiInboundRecognizeVO vo = new AiInboundRecognizeVO();
        vo.setRecordId(record.getId());
        vo.setTaskNo(record.getTaskNo());
        vo.setDocType(record.getDocType());
        vo.setRecognitionStatus(record.getRecognitionStatus());
        vo.setSourceFileName(record.getSourceFileName());
        vo.setSupplierName(record.getSupplierName());
        vo.setRawText(record.getRawText());
        vo.setWarningsJson(record.getWarningsJson());
        vo.setConfirmedOrderId(record.getConfirmedOrderId());
        vo.setItemList(itemVOList);

        if (savedResult != null) {
            vo.setMatchedSupplierId(savedResult.getMatchedSupplierId());
            vo.setSupplierMatchStatus(savedResult.getSupplierMatchStatus());
            vo.setWarnings(savedResult.getWarnings());
        }

        return vo;
    }

    public AiOutboundRecognizeVO getOutboundRecordDetail(Long recordId) {
        if (recordId == null) {
            throw new BusinessException("记录ID不能为空");
        }

        AiRecognitionRecord record = aiRecognitionRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("AI识别记录不存在");
        }
        if (!"outbound".equals(record.getDocType())) {
            throw new BusinessException("该识别记录不是出库单类型");
        }

        List<AiOutboundRecognizeItemVO> itemVOList = buildOutboundItemVOList(aiRecognitionItemMapper.selectByRecordId(recordId));
        AiOutboundRecognizeVO savedResult = readSavedOutboundRecognizeResult(record);

        AiOutboundRecognizeVO vo = new AiOutboundRecognizeVO();
        vo.setRecordId(record.getId());
        vo.setTaskNo(record.getTaskNo());
        vo.setDocType(record.getDocType());
        vo.setRecognitionStatus(record.getRecognitionStatus());
        vo.setSourceFileName(record.getSourceFileName());
        vo.setCustomerName(record.getSupplierName());
        vo.setRawText(record.getRawText());
        vo.setWarningsJson(record.getWarningsJson());
        vo.setConfirmedOrderId(record.getConfirmedOrderId());
        vo.setItemList(itemVOList);

        if (savedResult != null) {
            vo.setMatchedCustomerId(savedResult.getMatchedCustomerId());
            vo.setCustomerMatchStatus(savedResult.getCustomerMatchStatus());
            vo.setWarnings(savedResult.getWarnings());
            vo.setConfirmedOrderNo(savedResult.getConfirmedOrderNo());
        }

        return vo;
    }

    public void replaceItems(Long recordId, List<AiRecognitionItem> itemList) {
        aiRecognitionItemMapper.deleteByRecordId(recordId);
        if (itemList != null && !itemList.isEmpty()) {
            aiRecognitionItemMapper.batchInsert(itemList);
        }
    }

    public void appendItems(List<AiRecognitionItem> itemList) {
        if (itemList != null && !itemList.isEmpty()) {
            aiRecognitionItemMapper.batchInsert(itemList);
        }
    }

    public void updateStatus(Long id,
                             String recognitionStatus,
                             String errorMessage,
                             String supplierName,
                             String rawText,
                             String warningsJson,
                             String resultJson) {
        aiRecognitionRecordMapper.updateStatus(
                id,
                recognitionStatus,
                errorMessage,
                supplierName,
                rawText,
                warningsJson,
                resultJson
        );
    }

    public void markInboundRecognizedSuccess(AiRecognitionRecord record,
                                             String supplierName,
                                             String rawText,
                                             String warningsJson,
                                             AiInboundRecognizeVO resultVO) {
        updateStatus(
                record.getId(),
                STATUS_WAIT_MANUAL_CONFIRM,
                null,
                supplierName,
                rawText,
                warningsJson,
                toJson(resultVO)
        );
    }

    public void markOutboundRecognizedSuccess(AiRecognitionRecord record,
                                              String customerName,
                                              String rawText,
                                              String warningsJson,
                                              AiOutboundRecognizeVO resultVO) {
        updateStatus(
                record.getId(),
                STATUS_WAIT_MANUAL_CONFIRM,
                null,
                customerName,
                rawText,
                warningsJson,
                toJson(resultVO)
        );
    }

    public void saveEditedInboundDraft(AiRecognitionRecord record,
                                       AiInboundRecognizeVO resultVO,
                                       List<AiRecognitionItem> itemList) {
        replaceItems(record.getId(), itemList);
        updateStatus(
                record.getId(),
                STATUS_WAIT_MANUAL_CONFIRM,
                null,
                resultVO.getSupplierName(),
                resultVO.getRawText(),
                resultVO.getWarningsJson(),
                toJson(resultVO)
        );
    }

    public void saveEditedOutboundDraft(AiRecognitionRecord record,
                                        AiOutboundRecognizeVO resultVO,
                                        List<AiRecognitionItem> itemList) {
        replaceItems(record.getId(), itemList);
        updateStatus(
                record.getId(),
                STATUS_WAIT_MANUAL_CONFIRM,
                null,
                resultVO.getCustomerName(),
                resultVO.getRawText(),
                resultVO.getWarningsJson(),
                toJson(resultVO)
        );
    }

    public void markFailed(Long recordId, Exception e) {
        updateStatus(
                recordId,
                STATUS_FAILED,
                resolveErrorMessage(e),
                null,
                null,
                null,
                null
        );
    }

    public int markConfirmedToOrder(Long id, Long confirmedOrderId) {
        return aiRecognitionRecordMapper.updateConfirmedOrderId(id, confirmedOrderId, STATUS_CONFIRMED_TO_ORDER);
    }

    public boolean isPending(AiRecognitionRecord record) {
        return record != null && STATUS_PENDING.equals(record.getRecognitionStatus());
    }

    public boolean isFailed(AiRecognitionRecord record) {
        return record != null && STATUS_FAILED.equals(record.getRecognitionStatus());
    }

    public boolean isConfirmed(AiRecognitionRecord record) {
        return record != null && (record.getConfirmedOrderId() != null || STATUS_CONFIRMED_TO_ORDER.equals(record.getRecognitionStatus()));
    }

    private AiInboundRecognizeVO readSavedInboundRecognizeResult(AiRecognitionRecord record) {
        String resultJson = record.getResultJson();
        if (resultJson == null || resultJson.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(resultJson, AiInboundRecognizeVO.class);
        } catch (Exception e) {
            return null;
        }
    }

    private AiOutboundRecognizeVO readSavedOutboundRecognizeResult(AiRecognitionRecord record) {
        String resultJson = record.getResultJson();
        if (resultJson == null || resultJson.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(resultJson, AiOutboundRecognizeVO.class);
        } catch (Exception e) {
            return null;
        }
    }

    private List<AiInboundRecognizeItemVO> buildInboundItemVOList(List<AiRecognitionItem> itemEntities) {
        List<AiInboundRecognizeItemVO> itemVOList = new ArrayList<>();
        for (AiRecognitionItem item : itemEntities) {
            AiInboundRecognizeItemVO vo = new AiInboundRecognizeItemVO();
            vo.setLineNo(item.getLineNo());
            vo.setProductName(item.getProductName());
            vo.setSpecification(item.getSpecification());
            vo.setUnit(item.getUnit());
            vo.setQuantity(item.getQuantity());
            vo.setUnitPrice(item.getUnitPrice());
            vo.setAmount(item.getAmount());
            vo.setMatchedProductId(item.getMatchedProductId());
            vo.setMatchStatus(item.getMatchStatus());
            vo.setRemark(item.getRemark());
            itemVOList.add(vo);
        }
        return itemVOList;
    }

    private List<AiOutboundRecognizeItemVO> buildOutboundItemVOList(List<AiRecognitionItem> itemEntities) {
        List<AiOutboundRecognizeItemVO> itemVOList = new ArrayList<>();
        for (AiRecognitionItem item : itemEntities) {
            AiOutboundRecognizeItemVO vo = new AiOutboundRecognizeItemVO();
            vo.setLineNo(item.getLineNo());
            vo.setProductName(item.getProductName());
            vo.setSpecification(item.getSpecification());
            vo.setUnit(item.getUnit());
            vo.setQuantity(item.getQuantity());
            vo.setUnitPrice(item.getUnitPrice());
            vo.setAmount(item.getAmount());
            vo.setMatchedProductId(item.getMatchedProductId());
            vo.setMatchStatus(item.getMatchStatus());
            vo.setRemark(item.getRemark());
            itemVOList.add(vo);
        }
        return itemVOList;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new BusinessException("AI识别结果保存失败");
        }
    }

    private String resolveErrorMessage(Exception e) {
        if (e == null || e.getMessage() == null || e.getMessage().isBlank()) {
            return "AI识别失败";
        }
        String message = e.getMessage().trim();
        return message.length() > 200 ? message.substring(0, 200) : message;
    }

    private int normalizePageNum(Integer pageNum) {
        if (pageNum == null || pageNum < 1) {
            return 1;
        }
        return pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        int maxPageSize = 200;
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, maxPageSize);
    }
}
