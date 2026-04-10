package com.yocaihua.wms.service.ai;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.dto.AiInboundConfirmDTO;
import com.yocaihua.wms.dto.AiInboundConfirmItemDTO;
import com.yocaihua.wms.dto.AiOutboundConfirmDTO;
import com.yocaihua.wms.dto.AiOutboundConfirmItemDTO;
import com.yocaihua.wms.entity.AiRecognitionRecord;
import com.yocaihua.wms.entity.InboundOrder;
import com.yocaihua.wms.entity.OutboundOrder;
import com.yocaihua.wms.mapper.InboundOrderMapper;
import com.yocaihua.wms.mapper.OutboundOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AiRecognitionValidationService {

    private static final String DOC_TYPE_INBOUND = "inbound";
    private static final String DOC_TYPE_OUTBOUND = "outbound";

    private final AiRecognitionRecordService aiRecognitionRecordService;
    private final InboundOrderMapper inboundOrderMapper;
    private final OutboundOrderMapper outboundOrderMapper;

    public void validateInboundConfirmDTO(AiInboundConfirmDTO dto) {
        if (dto == null || dto.getRecordId() == null) {
            throw new BusinessException("识别记录ID不能为空");
        }
        if (dto.getItemList() == null || dto.getItemList().isEmpty()) {
            throw new BusinessException("确认明细不能为空");
        }
    }

    public void validateInboundConfirmItem(AiInboundConfirmItemDTO itemDTO) {
        if (itemDTO.getLineNo() == null || itemDTO.getLineNo() <= 0) {
            throw new BusinessException("明细行号必须大于0");
        }
        if (itemDTO.getMatchedProductId() == null) {
            throw new BusinessException("存在未匹配商品，不能确认生成入库单");
        }
        if (itemDTO.getQuantity() == null || itemDTO.getQuantity() <= 0) {
            throw new BusinessException("入库数量必须大于0");
        }
        if (itemDTO.getUnitPrice() == null || itemDTO.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("单价不能小于0");
        }
        if (itemDTO.getAmount() != null && itemDTO.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("金额不能小于0");
        }
    }

    public AiRecognitionRecord getAndValidateInboundRecord(Long recordId) {
        AiRecognitionRecord record = aiRecognitionRecordService.getByIdForUpdate(recordId);
        if (record == null) {
            throw new BusinessException("AI识别记录不存在");
        }
        if (!DOC_TYPE_INBOUND.equals(record.getDocType())) {
            throw new BusinessException("该识别记录不是入库单类型");
        }
        if (aiRecognitionRecordService.isConfirmed(record)) {
            throw buildInboundAlreadyConfirmedException(record);
        }
        if (aiRecognitionRecordService.isPending(record)) {
            throw new BusinessException("AI识别尚未完成，请稍后重试");
        }
        if (aiRecognitionRecordService.isFailed(record)) {
            throw new BusinessException("AI识别失败，不能确认生成入库单");
        }
        return record;
    }

    public BusinessException buildInboundAlreadyConfirmedException(AiRecognitionRecord record) {
        if (record != null && record.getConfirmedOrderId() != null) {
            InboundOrder inboundOrder = inboundOrderMapper.selectById(record.getConfirmedOrderId());
            if (inboundOrder != null && inboundOrder.getOrderNo() != null) {
                return new BusinessException("该AI记录已生成正式入库单，单号：" + inboundOrder.getOrderNo() + "，不能重复确认");
            }
        }
        return new BusinessException("该AI记录已确认生成入库单，不能重复确认");
    }

    public void validateOutboundConfirmDTO(AiOutboundConfirmDTO dto) {
        if (dto == null || dto.getRecordId() == null) {
            throw new BusinessException("识别记录ID不能为空");
        }
        if (dto.getItemList() == null || dto.getItemList().isEmpty()) {
            throw new BusinessException("确认明细不能为空");
        }
    }

    public void validateOutboundConfirmItem(AiOutboundConfirmItemDTO itemDTO) {
        if (itemDTO.getLineNo() == null || itemDTO.getLineNo() <= 0) {
            throw new BusinessException("明细行号必须大于0");
        }
        if (itemDTO.getMatchedProductId() == null) {
            throw new BusinessException("存在未匹配商品，不能确认生成出库单");
        }
        if (itemDTO.getQuantity() == null || itemDTO.getQuantity() <= 0) {
            throw new BusinessException("出库数量必须大于0");
        }
        if (itemDTO.getUnitPrice() == null || itemDTO.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("单价不能小于0");
        }
        if (itemDTO.getAmount() != null && itemDTO.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("金额不能小于0");
        }
    }

    public AiRecognitionRecord getAndValidateOutboundRecord(Long recordId) {
        AiRecognitionRecord record = aiRecognitionRecordService.getByIdForUpdate(recordId);
        if (record == null) {
            throw new BusinessException("AI识别记录不存在");
        }
        if (!DOC_TYPE_OUTBOUND.equals(record.getDocType())) {
            throw new BusinessException("该识别记录不是出库单类型");
        }
        if (aiRecognitionRecordService.isConfirmed(record)) {
            throw buildOutboundAlreadyConfirmedException(record);
        }
        if (aiRecognitionRecordService.isPending(record)) {
            throw new BusinessException("AI识别尚未完成，请稍后重试");
        }
        if (aiRecognitionRecordService.isFailed(record)) {
            throw new BusinessException("AI识别失败，不能确认生成出库单");
        }
        return record;
    }

    public BusinessException buildOutboundAlreadyConfirmedException(AiRecognitionRecord record) {
        if (record != null && record.getConfirmedOrderId() != null) {
            OutboundOrder outboundOrder = outboundOrderMapper.selectById(record.getConfirmedOrderId());
            if (outboundOrder != null && outboundOrder.getOrderNo() != null) {
                return new BusinessException("该AI记录已生成正式出库单，单号：" + outboundOrder.getOrderNo() + "，不能重复确认");
            }
        }
        return new BusinessException("该AI记录已确认生成出库单，不能重复确认");
    }
}
