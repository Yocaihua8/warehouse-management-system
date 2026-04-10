package com.yocaihua.wms.service;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.dto.AiInboundConfirmDTO;
import com.yocaihua.wms.dto.AiOutboundConfirmDTO;
import com.yocaihua.wms.vo.AiInboundRecognizeVO;
import com.yocaihua.wms.vo.AiOutboundRecognizeVO;
import com.yocaihua.wms.vo.AiRecognitionRecordVO;
import org.springframework.web.multipart.MultipartFile;

public interface AiRecognitionService {

    AiInboundRecognizeVO recognizeInbound(MultipartFile file, String operator);

    Long confirmInbound(AiInboundConfirmDTO dto, String operator);

    AiOutboundRecognizeVO recognizeOutbound(MultipartFile file, String operator);

    Long confirmOutbound(AiOutboundConfirmDTO dto, String operator);

    PageResult<AiRecognitionRecordVO> listInboundRecords(Integer pageNum, Integer pageSize);

    AiInboundRecognizeVO getInboundRecordDetail(Long recordId);

    PageResult<AiRecognitionRecordVO> listOutboundRecords(Integer pageNum, Integer pageSize);

    AiOutboundRecognizeVO getOutboundRecordDetail(Long recordId);
}
