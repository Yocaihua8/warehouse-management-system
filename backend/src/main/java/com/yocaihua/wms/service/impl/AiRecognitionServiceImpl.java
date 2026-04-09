package com.yocaihua.wms.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yocaihua.wms.dto.AiInboundConfirmDTO;
import com.yocaihua.wms.dto.AiInboundConfirmItemDTO;
import com.yocaihua.wms.dto.AiOutboundConfirmDTO;
import com.yocaihua.wms.dto.AiOutboundConfirmItemDTO;
import com.yocaihua.wms.dto.PythonOcrItemDTO;
import com.yocaihua.wms.dto.PythonOcrRecognizeDataDTO;
import com.yocaihua.wms.entity.AiRecognitionItem;
import com.yocaihua.wms.entity.AiRecognitionRecord;
import com.yocaihua.wms.entity.Customer;
import com.yocaihua.wms.entity.InboundOrder;
import com.yocaihua.wms.entity.InboundOrderItem;
import com.yocaihua.wms.entity.OutboundOrder;
import com.yocaihua.wms.entity.OutboundOrderItem;
import com.yocaihua.wms.entity.Supplier;
import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.OperationLogActionConstant;
import com.yocaihua.wms.common.OrderStatusConstant;
import com.yocaihua.wms.common.StockChangeTypeConstant;
import com.yocaihua.wms.mapper.*;
import com.yocaihua.wms.service.ai.AiDraftPersistenceService;
import com.yocaihua.wms.service.ai.AiRecognitionRecordService;
import com.yocaihua.wms.service.ai.AiOrderAssemblerService;
import com.yocaihua.wms.service.ai.OcrAdapterService;
import com.yocaihua.wms.service.ai.ProductMatchService;
import com.yocaihua.wms.vo.AiInboundRecognizeItemVO;
import com.yocaihua.wms.service.AiRecognitionService;
import com.yocaihua.wms.service.OperationLogService;
import com.yocaihua.wms.service.StockFlowService;
import com.yocaihua.wms.vo.AiInboundRecognizeVO;
import com.yocaihua.wms.vo.AiOutboundRecognizeItemVO;
import com.yocaihua.wms.vo.AiOutboundRecognizeVO;
import com.yocaihua.wms.vo.AiRecognitionRecordVO;
import com.yocaihua.wms.vo.StockVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiRecognitionServiceImpl implements AiRecognitionService {

    private static final String DOC_TYPE_INBOUND = "inbound";
    private static final String DOC_TYPE_OUTBOUND = "outbound";
    private static final String STATUS_WAIT_MANUAL_CONFIRM = "success";

    private final AiRecognitionRecordService aiRecognitionRecordService;
    private final InboundOrderMapper inboundOrderMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final StockMapper stockMapper;
    private final ObjectMapper objectMapper;
    private final InboundOrderItemMapper inboundOrderItemMapper;
    private final OutboundOrderItemMapper outboundOrderItemMapper;
    private final OcrAdapterService ocrAdapterService;
    private final AiOrderAssemblerService aiOrderAssemblerService;
    private final ProductMatchService productMatchService;
    private final AiDraftPersistenceService aiDraftPersistenceService;
    private final CustomerMapper customerMapper;
    private final SupplierMapper supplierMapper;
    private final StockFlowService stockFlowService;
    private final OperationLogService operationLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long confirmInbound(AiInboundConfirmDTO dto, String operator) {
        validateConfirmDTO(dto);

        AiRecognitionRecord record = getAndValidateInboundRecord(dto.getRecordId());
        Supplier supplier = resolveSupplier(dto);
        String supplierName = resolveSupplierName(dto, record, supplier);
        String rawText = resolveRawText(dto.getRawText(), record.getRawText());
        List<AiInboundRecognizeItemVO> editedItemVOList = buildEditedInboundItemVOList(dto.getItemList());
        ProductMatchService.SupplierMatchResult supplierMatchResult = buildManualConfirmedSupplierMatchResult(supplier, supplierName);

        aiDraftPersistenceService.saveEditedInboundDraft(
                record,
                supplierName,
                supplierMatchResult.getSupplierId(),
                supplierMatchResult.getMatchStatus(),
                rawText,
                editedItemVOList
        );

        InboundOrder inboundOrder = aiOrderAssemblerService.buildInboundOrder(
                supplierMatchResult.getSupplierId(),
                supplierName,
                dto.getRemark()
        );

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<InboundOrderItem> orderItemList = new ArrayList<>();

        for (AiInboundConfirmItemDTO itemDTO : dto.getItemList()) {
            validateConfirmItem(itemDTO);

            BigDecimal amount = aiOrderAssemblerService.resolveItemAmount(itemDTO.getAmount(), itemDTO.getQuantity(), itemDTO.getUnitPrice());
            totalAmount = totalAmount.add(amount);

            InboundOrderItem orderItem = aiOrderAssemblerService.buildInboundOrderItem(itemDTO, amount);
            orderItemList.add(orderItem);
        }

        inboundOrder.setTotalAmount(totalAmount);
        inboundOrderMapper.insert(inboundOrder);

        for (InboundOrderItem item : orderItemList) {
            item.setInboundOrderId(inboundOrder.getId());
        }
        inboundOrderItemMapper.insertBatch(orderItemList);

        stockFlowService.increaseByInbound(
                inboundOrder.getId(),
                inboundOrder.getOrderNo(),
                orderItemList,
                operator,
                StockChangeTypeConstant.AI_CONFIRM_INBOUND,
                dto.getRemark()
        );

        int rows = aiRecognitionRecordService.markConfirmedToOrder(dto.getRecordId(), inboundOrder.getId());
        if (rows <= 0) {
            throw buildInboundAlreadyConfirmedException(aiRecognitionRecordService.getById(dto.getRecordId()));
        }
        operationLogService.recordSuccess(
                OperationLogActionConstant.AI_INBOUND_CONFIRM,
                "AI识别入库",
                "INBOUND_ORDER",
                inboundOrder.getId(),
                inboundOrder.getOrderNo(),
                operator,
                "AI确认生成入库单，recordId=" + dto.getRecordId()
        );

        return inboundOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long confirmOutbound(AiOutboundConfirmDTO dto, String operator) {
        validateOutboundConfirmDTO(dto);

        AiRecognitionRecord record = getAndValidateOutboundRecord(dto.getRecordId());
        Customer customer = resolveCustomer(dto, record);
        String customerDisplayName = resolveCustomerDisplayName(dto, customer, record);
        String rawText = resolveRawText(dto.getRawText(), record.getRawText());
        List<AiOutboundRecognizeItemVO> editedItemVOList = buildEditedOutboundItemVOList(dto.getItemList());

        aiDraftPersistenceService.saveEditedOutboundDraft(record, customerDisplayName, rawText, customer.getId(), editedItemVOList);

        OutboundOrder outboundOrder = aiOrderAssemblerService.buildOutboundOrder(
                customer.getId(),
                customerDisplayName,
                dto.getRemark()
        );

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OutboundOrderItem> orderItemList = new ArrayList<>();

        for (AiOutboundConfirmItemDTO itemDTO : dto.getItemList()) {
            validateOutboundConfirmItem(itemDTO);

            BigDecimal amount = aiOrderAssemblerService.resolveItemAmount(itemDTO.getAmount(), itemDTO.getQuantity(), itemDTO.getUnitPrice());
            totalAmount = totalAmount.add(amount);

            OutboundOrderItem orderItem = aiOrderAssemblerService.buildOutboundOrderItem(itemDTO, amount);
            orderItemList.add(orderItem);
        }

        outboundOrder.setTotalAmount(totalAmount);
        outboundOrderMapper.insert(outboundOrder);

        for (OutboundOrderItem item : orderItemList) {
            item.setOutboundOrderId(outboundOrder.getId());
            int itemRows = outboundOrderItemMapper.insert(item);
            if (itemRows <= 0) {
                throw new BusinessException("保存出库单明细失败");
            }
        }

        stockFlowService.decreaseByOutbound(
                outboundOrder.getId(),
                outboundOrder.getOrderNo(),
                orderItemList,
                operator,
                StockChangeTypeConstant.AI_CONFIRM_OUTBOUND,
                dto.getRemark()
        );

        int rows = aiRecognitionRecordService.markConfirmedToOrder(dto.getRecordId(), outboundOrder.getId());
        if (rows <= 0) {
            throw buildOutboundAlreadyConfirmedException(aiRecognitionRecordService.getById(dto.getRecordId()));
        }
        operationLogService.recordSuccess(
                OperationLogActionConstant.AI_OUTBOUND_CONFIRM,
                "AI识别出库",
                "OUTBOUND_ORDER",
                outboundOrder.getId(),
                outboundOrder.getOrderNo(),
                operator,
                "AI确认生成出库单，recordId=" + dto.getRecordId()
        );

        return outboundOrder.getId();
    }

    @Override
    public List<AiRecognitionRecordVO> listInboundRecords() {
        return aiRecognitionRecordService.listInboundRecords();
    }

    @Override
    public List<AiRecognitionRecordVO> listOutboundRecords() {
        return aiRecognitionRecordService.listOutboundRecords();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiOutboundRecognizeVO recognizeOutbound(MultipartFile file, String operator) {
        String taskNo = buildTaskNo();
        String fileName = file != null ? file.getOriginalFilename() : "outbound.jpg";

        AiRecognitionRecord record = aiRecognitionRecordService.createPendingRecord(taskNo, fileName, operator, DOC_TYPE_OUTBOUND);
        try {
            PythonOcrRecognizeDataDTO ocrData = ocrAdapterService.recognizeOutbound(file);
            List<AiOutboundRecognizeItemVO> itemVOList = convertPythonItemsToOutboundAiItems(ocrData.getItems());

            matchOutboundProducts(itemVOList);

            List<AiRecognitionItem> itemList = aiDraftPersistenceService.convertOutboundItemsToEntities(record.getId(), itemVOList);
            aiRecognitionRecordService.appendItems(itemList);

            String customerName = resolveRecognizedCustomerName(ocrData);
            ProductMatchService.CustomerMatchResult customerMatchResult = findMatchedCustomer(customerName);
            String rawText = ocrData.getRawText();
            String warningsJson = toJson(ocrData.getWarnings());

            AiOutboundRecognizeVO resultVO = new AiOutboundRecognizeVO();
            resultVO.setRecordId(record.getId());
            resultVO.setTaskNo(record.getTaskNo());
            resultVO.setDocType(DOC_TYPE_OUTBOUND);
            resultVO.setRecognitionStatus(STATUS_WAIT_MANUAL_CONFIRM);
            resultVO.setSourceFileName(fileName);
            resultVO.setCustomerName(customerName);
            resultVO.setMatchedCustomerId(customerMatchResult.getCustomerId());
            resultVO.setCustomerMatchStatus(customerMatchResult.getMatchStatus());
            resultVO.setRawText(rawText);
            resultVO.setWarningsJson(warningsJson);
            resultVO.setWarnings(ocrData.getWarnings());
            resultVO.setConfirmedOrderId(record.getConfirmedOrderId());
            resultVO.setItemList(itemVOList);

            aiRecognitionRecordService.markOutboundRecognizedSuccess(
                    record,
                    customerName,
                    rawText,
                    warningsJson,
                    resultVO
            );

            return resultVO;
        } catch (Exception e) {
            markRecordFailed(record.getId(), e);
            throw propagateRecognitionException(e);
        }
    }

    @Override
    public AiInboundRecognizeVO getInboundRecordDetail(Long recordId) {
        AiInboundRecognizeVO vo = aiRecognitionRecordService.getInboundRecordDetail(recordId);
        ProductMatchService.SupplierMatchResult supplierMatchResult = findMatchedSupplier(vo.getSupplierName());

        Long matchedSupplierId = vo.getMatchedSupplierId() != null
                ? vo.getMatchedSupplierId()
                : supplierMatchResult.getSupplierId();
        String supplierMatchStatus = vo.getSupplierMatchStatus() != null
                ? vo.getSupplierMatchStatus()
                : supplierMatchResult.getMatchStatus();

        vo.setMatchedSupplierId(matchedSupplierId);
        vo.setSupplierMatchStatus(supplierMatchStatus);
        if (vo.getConfirmedOrderId() != null) {
            InboundOrder inboundOrder = inboundOrderMapper.selectById(vo.getConfirmedOrderId());
            if (inboundOrder != null) {
                vo.setConfirmedOrderNo(inboundOrder.getOrderNo());
            }
        }

        return vo;
    }

    @Override
    public AiOutboundRecognizeVO getOutboundRecordDetail(Long recordId) {
        AiOutboundRecognizeVO vo = aiRecognitionRecordService.getOutboundRecordDetail(recordId);
        if (vo.getConfirmedOrderId() != null) {
            OutboundOrder outboundOrder = outboundOrderMapper.selectById(vo.getConfirmedOrderId());
            if (outboundOrder != null) {
                vo.setConfirmedOrderNo(outboundOrder.getOrderNo());
            }
        }
        return vo;
    }

    private AiInboundRecognizeVO buildRecognizeResult(AiRecognitionRecord record,
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiInboundRecognizeVO recognizeInbound(MultipartFile file, String operator) {
        String taskNo = buildTaskNo();
        String fileName = file != null ? file.getOriginalFilename() : "inbound.jpg";

        AiRecognitionRecord record = aiRecognitionRecordService.createPendingRecord(taskNo, fileName, operator, DOC_TYPE_INBOUND);
        try {
            PythonOcrRecognizeDataDTO ocrData = ocrAdapterService.recognizeInbound(file);
            List<AiInboundRecognizeItemVO> itemVOList = convertPythonItemsToAiItems(ocrData.getItems());

            matchProducts(itemVOList);
            ProductMatchService.SupplierMatchResult supplierMatchResult = findMatchedSupplier(ocrData.getSupplierName());

            List<AiRecognitionItem> itemList = aiDraftPersistenceService.convertInboundItemsToEntities(record.getId(), itemVOList);
            aiRecognitionRecordService.appendItems(itemList);

            String rawText = ocrData.getRawText();
            String warningsJson = toJson(ocrData.getWarnings());

            AiInboundRecognizeVO resultVO = buildRecognizeResult(
                    record,
                    fileName,
                    ocrData.getSupplierName(),
                    supplierMatchResult.getSupplierId(),
                    supplierMatchResult.getMatchStatus(),
                    rawText,
                    warningsJson,
                    itemVOList
            );

            aiRecognitionRecordService.markInboundRecognizedSuccess(
                    record,
                    resultVO.getSupplierName(),
                    rawText,
                    warningsJson,
                    resultVO
            );

            return resultVO;
        } catch (Exception e) {
            markRecordFailed(record.getId(), e);
            throw propagateRecognitionException(e);
        }
    }

    private void validateConfirmDTO(AiInboundConfirmDTO dto) {
        if (dto == null || dto.getRecordId() == null) {
            throw new BusinessException("识别记录ID不能为空");
        }
        if (dto.getItemList() == null || dto.getItemList().isEmpty()) {
            throw new BusinessException("确认明细不能为空");
        }
    }

    private AiRecognitionRecord getAndValidateInboundRecord(Long recordId) {
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

    private BusinessException buildInboundAlreadyConfirmedException(AiRecognitionRecord record) {
        if (record != null && record.getConfirmedOrderId() != null) {
            InboundOrder inboundOrder = inboundOrderMapper.selectById(record.getConfirmedOrderId());
            if (inboundOrder != null && inboundOrder.getOrderNo() != null) {
                return new BusinessException("该AI记录已生成正式入库单，单号：" + inboundOrder.getOrderNo() + "，不能重复确认");
            }
        }
        return new BusinessException("该AI记录已确认生成入库单，不能重复确认");
    }

    private AiRecognitionRecord getAndValidateOutboundRecord(Long recordId) {
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

    private BusinessException buildOutboundAlreadyConfirmedException(AiRecognitionRecord record) {
        if (record != null && record.getConfirmedOrderId() != null) {
            OutboundOrder outboundOrder = outboundOrderMapper.selectById(record.getConfirmedOrderId());
            if (outboundOrder != null && outboundOrder.getOrderNo() != null) {
                return new BusinessException("该AI记录已生成正式出库单，单号：" + outboundOrder.getOrderNo() + "，不能重复确认");
            }
        }
        return new BusinessException("该AI记录已确认生成出库单，不能重复确认");
    }

    private Supplier resolveSupplier(AiInboundConfirmDTO dto) {
        if (dto.getSupplierId() == null) {
            return null;
        }

        Supplier supplier = supplierMapper.selectById(dto.getSupplierId());
        if (supplier == null) {
            throw new BusinessException("供应商不存在");
        }

        return supplier;
    }

    private String resolveSupplierName(AiInboundConfirmDTO dto, AiRecognitionRecord record, Supplier supplier) {
        if (supplier != null) {
            return supplier.getSupplierName();
        }

        String supplierName = dto.getSupplierName();
        if (supplierName != null) {
            supplierName = supplierName.trim();
        }
        if (supplierName == null || supplierName.isEmpty()) {
            supplierName = record.getSupplierName();
        }
        if (supplierName == null || supplierName.trim().isEmpty()) {
            throw new BusinessException("供应商名称不能为空");
        }
        return supplierName;
    }

    private void validateConfirmItem(AiInboundConfirmItemDTO itemDTO) {
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

    private void validateOutboundConfirmDTO(AiOutboundConfirmDTO dto) {
        if (dto == null || dto.getRecordId() == null) {
            throw new BusinessException("识别记录ID不能为空");
        }
        if (dto.getItemList() == null || dto.getItemList().isEmpty()) {
            throw new BusinessException("确认明细不能为空");
        }
    }

    private void validateOutboundConfirmItem(AiOutboundConfirmItemDTO itemDTO) {
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

    private Customer resolveCustomer(AiOutboundConfirmDTO dto, AiRecognitionRecord record) {
        if (dto.getCustomerId() != null) {
            Customer customer = customerMapper.selectById(dto.getCustomerId());
            if (customer == null) {
                throw new BusinessException("客户不存在");
            }
            return customer;
        }

        String customerName = dto.getCustomerName();
        if (customerName != null) {
            customerName = customerName.trim();
        }
        if (customerName == null || customerName.isEmpty()) {
            customerName = record.getSupplierName();
        }
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new BusinessException("客户名称不能为空");
        }

        ProductMatchService.CustomerMatchResult matchResult = findMatchedCustomer(customerName);
        if (matchResult.getCustomerId() == null) {
            throw new BusinessException("未匹配到客户，请先人工选择客户");
        }

        Customer customer = customerMapper.selectById(matchResult.getCustomerId());
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }
        return customer;
    }

    private String resolveCustomerDisplayName(AiOutboundConfirmDTO dto, Customer customer, AiRecognitionRecord record) {
        String customerName = dto.getCustomerName();
        if (customerName != null) {
            customerName = customerName.trim();
        }
        if (customerName == null || customerName.isEmpty()) {
            customerName = customer != null ? customer.getCustomerName() : record.getSupplierName();
        }
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new BusinessException("客户名称不能为空");
        }
        return customerName;
    }

    private void markRecordFailed(Long recordId, Exception e) {
        aiRecognitionRecordService.markFailed(recordId, e);
    }

    private RuntimeException propagateRecognitionException(Exception e) {
        if (e instanceof BusinessException businessException) {
            return businessException;
        }
        String message = e == null || e.getMessage() == null || e.getMessage().isBlank()
                ? "AI识别失败"
                : e.getMessage().trim();
        return new BusinessException(message.length() > 200 ? message.substring(0, 200) : message);
    }

    private String buildTaskNo() {
        return "AIR" + System.currentTimeMillis();
    }

    private String resolveRawText(String dtoRawText, String recordRawText) {
        String rawText = normalizeOptionalText(dtoRawText);
        return rawText != null ? rawText : recordRawText;
    }

    private String normalizeOptionalText(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private List<AiInboundRecognizeItemVO> buildEditedInboundItemVOList(List<AiInboundConfirmItemDTO> itemList) {
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

    private List<AiOutboundRecognizeItemVO> buildEditedOutboundItemVOList(List<AiOutboundConfirmItemDTO> itemList) {
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

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    private String resolveRecognizedCustomerName(PythonOcrRecognizeDataDTO ocrData) {
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

    private List<AiInboundRecognizeItemVO> convertPythonItemsToAiItems(List<PythonOcrItemDTO> pythonItems) {
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

    private List<AiOutboundRecognizeItemVO> convertPythonItemsToOutboundAiItems(List<PythonOcrItemDTO> pythonItems) {
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

    private void matchProducts(List<AiInboundRecognizeItemVO> itemVOList) {
        productMatchService.matchInboundProducts(itemVOList);
    }

    private void matchOutboundProducts(List<AiOutboundRecognizeItemVO> itemVOList) {
        productMatchService.matchOutboundProducts(itemVOList);
    }

    private ProductMatchService.SupplierMatchResult findMatchedSupplier(String supplierName) {
        return productMatchService.findMatchedSupplier(supplierName);
    }

    private ProductMatchService.SupplierMatchResult buildManualConfirmedSupplierMatchResult(Supplier supplier, String supplierName) {
        if (supplier != null) {
            return new ProductMatchService.SupplierMatchResult(supplier.getId(), "manual_confirmed", "人工确认供应商");
        }
        return findMatchedSupplier(supplierName);
    }

    private ProductMatchService.CustomerMatchResult findMatchedCustomer(String customerName) {
        return productMatchService.findMatchedCustomer(customerName);
    }
}
