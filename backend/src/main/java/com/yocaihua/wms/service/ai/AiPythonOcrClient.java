package com.yocaihua.wms.service.ai;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.dto.PythonOcrRecognizeDataDTO;
import com.yocaihua.wms.dto.PythonOcrRecognizeResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AiPythonOcrClient {

    @Value("${ai.python.base-url:http://127.0.0.1:9000}")
    private String aiPythonBaseUrl;

    private final RestTemplate restTemplate;

    public PythonOcrRecognizeDataDTO recognizeInbound(MultipartFile file) {
        return recognize(file, "/ocr/inbound/recognize");
    }

    public PythonOcrRecognizeDataDTO recognizeOutbound(MultipartFile file) {
        return recognize(file, "/ocr/outbound/recognize");
    }

    private PythonOcrRecognizeDataDTO recognize(MultipartFile file, String path) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<PythonOcrRecognizeResponseDTO> response = restTemplate.exchange(
                    buildRecognizeUrl(path),
                    HttpMethod.POST,
                    requestEntity,
                    PythonOcrRecognizeResponseDTO.class
            );

            PythonOcrRecognizeResponseDTO responseBody = response.getBody();
            if (responseBody == null || Boolean.FALSE.equals(responseBody.getSuccess()) || responseBody.getData() == null) {
                throw new BusinessException("Python OCR服务返回为空");
            }

            return responseBody.getData();
        } catch (ResourceAccessException e) {
            throw new BusinessException("调用Python OCR服务超时或不可达，请检查AI服务状态与超时配置");
        } catch (Exception e) {
            throw new BusinessException("调用Python OCR服务失败：" + e.getMessage());
        }
    }

    private String buildRecognizeUrl(String path) {
        String normalizedBaseUrl = aiPythonBaseUrl == null ? "" : aiPythonBaseUrl.trim();
        if (normalizedBaseUrl.isEmpty()) {
            normalizedBaseUrl = "http://127.0.0.1:9000";
        }

        if (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }

        if (path == null || path.isBlank()) {
            return normalizedBaseUrl;
        }

        return path.startsWith("/") ? normalizedBaseUrl + path : normalizedBaseUrl + "/" + path;
    }
}
