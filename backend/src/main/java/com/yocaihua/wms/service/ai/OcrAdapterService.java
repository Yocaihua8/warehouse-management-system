package com.yocaihua.wms.service.ai;

import com.yocaihua.wms.dto.PythonOcrRecognizeDataDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class OcrAdapterService {

    private final AiPythonOcrClient aiPythonOcrClient;

    public PythonOcrRecognizeDataDTO recognizeInbound(MultipartFile file) {
        return aiPythonOcrClient.recognizeInbound(file);
    }

    public PythonOcrRecognizeDataDTO recognizeOutbound(MultipartFile file) {
        return aiPythonOcrClient.recognizeOutbound(file);
    }
}
