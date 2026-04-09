package com.yocaihua.wms.service.ai;

import com.yocaihua.wms.entity.AiRecognitionItem;
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
public class AiDraftPersistenceService {

    private static final String STATUS_WAIT_MANUAL_CONFIRM = "success";

    private final AiRecognitionRecordService aiRecognitionRecordService;

    public void saveEditedInboundDraft(AiRecognitionRecord record,
                                       String supplierName,
                                       Long matchedSupplierId,
                                       String supplierMatchStatus,
                                       String rawText,
                                       List<AiInboundRecognizeItemVO> itemVOList) {
        AiInboundRecognizeVO resultVO = new AiInboundRecognizeVO();
        resultVO.setRecordId(record.getId());
        resultVO.setTaskNo(record.getTaskNo());
        resultVO.setDocType(record.getDocType());
        resultVO.setRecognitionStatus(STATUS_WAIT_MANUAL_CONFIRM);
        resultVO.setSourceFileName(record.getSourceFileName());
        resultVO.setSupplierName(supplierName);
        resultVO.setMatchedSupplierId(matchedSupplierId);
        resultVO.setSupplierMatchStatus(supplierMatchStatus);
        resultVO.setRawText(rawText);
        resultVO.setWarningsJson(record.getWarningsJson());
        resultVO.setConfirmedOrderId(record.getConfirmedOrderId());
        resultVO.setItemList(itemVOList);

        aiRecognitionRecordService.saveEditedInboundDraft(
                record,
                resultVO,
                convertInboundItemsToEntities(record.getId(), itemVOList)
        );
    }

    public void saveEditedOutboundDraft(AiRecognitionRecord record,
                                        String customerName,
                                        String rawText,
                                        Long matchedCustomerId,
                                        List<AiOutboundRecognizeItemVO> itemVOList) {
        AiOutboundRecognizeVO resultVO = new AiOutboundRecognizeVO();
        resultVO.setRecordId(record.getId());
        resultVO.setTaskNo(record.getTaskNo());
        resultVO.setDocType(record.getDocType());
        resultVO.setRecognitionStatus(STATUS_WAIT_MANUAL_CONFIRM);
        resultVO.setSourceFileName(record.getSourceFileName());
        resultVO.setCustomerName(customerName);
        resultVO.setMatchedCustomerId(matchedCustomerId);
        resultVO.setCustomerMatchStatus(matchedCustomerId != null ? "manual_confirmed" : "unmatched");
        resultVO.setRawText(rawText);
        resultVO.setWarningsJson(record.getWarningsJson());
        resultVO.setConfirmedOrderId(record.getConfirmedOrderId());
        resultVO.setItemList(itemVOList);

        aiRecognitionRecordService.saveEditedOutboundDraft(
                record,
                resultVO,
                convertOutboundItemsToEntities(record.getId(), itemVOList)
        );
    }

    public List<AiRecognitionItem> convertInboundItemsToEntities(Long recordId, List<AiInboundRecognizeItemVO> itemVOList) {
        List<AiRecognitionItem> entityList = new ArrayList<>();
        for (AiInboundRecognizeItemVO vo : itemVOList) {
            AiRecognitionItem item = new AiRecognitionItem();
            item.setRecordId(recordId);
            item.setLineNo(vo.getLineNo());
            item.setProductName(vo.getProductName());
            item.setSpecification(vo.getSpecification());
            item.setUnit(vo.getUnit());
            item.setQuantity(vo.getQuantity());
            item.setUnitPrice(vo.getUnitPrice());
            item.setAmount(vo.getAmount());
            item.setMatchedProductId(vo.getMatchedProductId());
            item.setMatchStatus(vo.getMatchStatus());
            item.setRemark(vo.getRemark());
            entityList.add(item);
        }
        return entityList;
    }

    public List<AiRecognitionItem> convertOutboundItemsToEntities(Long recordId, List<AiOutboundRecognizeItemVO> itemVOList) {
        List<AiRecognitionItem> entityList = new ArrayList<>();
        for (AiOutboundRecognizeItemVO vo : itemVOList) {
            AiRecognitionItem item = new AiRecognitionItem();
            item.setRecordId(recordId);
            item.setLineNo(vo.getLineNo());
            item.setProductName(vo.getProductName());
            item.setSpecification(vo.getSpecification());
            item.setUnit(vo.getUnit());
            item.setQuantity(vo.getQuantity());
            item.setUnitPrice(vo.getUnitPrice());
            item.setAmount(vo.getAmount());
            item.setMatchedProductId(vo.getMatchedProductId());
            item.setMatchStatus(vo.getMatchStatus());
            item.setRemark(vo.getRemark());
            entityList.add(item);
        }
        return entityList;
    }
}
