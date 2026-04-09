package com.yocaihua.wms.service;

import com.yocaihua.wms.dto.AiInboundConfirmDTO;
import com.yocaihua.wms.dto.AiOutboundConfirmDTO;
import com.yocaihua.wms.vo.AiInboundRecognizeVO;
import com.yocaihua.wms.vo.AiOutboundRecognizeVO;
import com.yocaihua.wms.vo.AiRecognitionRecordVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AiRecognitionService {

    AiInboundRecognizeVO recognizeInbound(MultipartFile file, String operator);

    Long confirmInbound(AiInboundConfirmDTO dto, String operator);

    AiOutboundRecognizeVO recognizeOutbound(MultipartFile file, String operator);

    Long confirmOutbound(AiOutboundConfirmDTO dto, String operator);

    List<AiRecognitionRecordVO> listInboundRecords();

    AiInboundRecognizeVO getInboundRecordDetail(Long recordId);

    List<AiRecognitionRecordVO> listOutboundRecords();

    AiOutboundRecognizeVO getOutboundRecordDetail(Long recordId);
}
