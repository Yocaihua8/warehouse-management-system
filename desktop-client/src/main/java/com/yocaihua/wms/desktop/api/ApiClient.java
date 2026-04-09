package com.yocaihua.wms.desktop.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yocaihua.wms.desktop.config.AppConfigService;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

public class ApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AppConfigService appConfigService;

    public ApiClient(AppConfigService appConfigService) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.appConfigService = appConfigService;
    }

    public <T> ApiResponse<T> get(String path, Class<T> dataType) {
        HttpRequest request = createRequestBuilder(path)
                .GET()
                .build();
        return send(request, dataType);
    }

    public <T> ApiResponse<List<T>> getList(String path, Class<T> itemType) {
        HttpRequest request = createRequestBuilder(path)
                .GET()
                .build();
        return sendList(request, itemType);
    }

    public <T> ApiResponse<T> get(String path, Map<String, ?> queryParams, Class<T> dataType) {
        HttpRequest request = createRequestBuilder(appendQueryParams(path, queryParams))
                .GET()
                .build();
        return send(request, dataType);
    }

    public <T> ApiResponse<T> post(String path, Object requestBody, Class<T> dataType) {
        String jsonBody = toJson(requestBody);
        HttpRequest request = createRequestBuilder(path)
                .header("Content-Type", "application/json;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
        return send(request, dataType);
    }

    public <T> ApiResponse<T> post(String path, Map<String, ?> queryParams, Object requestBody, Class<T> dataType) {
        String jsonBody = toJson(requestBody);
        HttpRequest request = createRequestBuilder(appendQueryParams(path, queryParams))
                .header("Content-Type", "application/json;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
        return send(request, dataType);
    }

    public <T> ApiResponse<T> put(String path, Object requestBody, Class<T> dataType) {
        String jsonBody = toJson(requestBody);
        HttpRequest request = createRequestBuilder(path)
                .header("Content-Type", "application/json;charset=UTF-8")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
        return send(request, dataType);
    }

    public <T> ApiResponse<T> delete(String path, Class<T> dataType) {
        HttpRequest request = createRequestBuilder(path)
                .DELETE()
                .build();
        return send(request, dataType);
    }

    public <T> ApiResponse<T> postMultipart(String path, String fieldName, Path filePath, Class<T> dataType) {
        if (filePath == null) {
            throw new ApiException("上传文件不能为空");
        }

        try {
            String boundary = "----WmsDesktopBoundary" + UUID.randomUUID().toString().replace("-", "");
            String fileName = filePath.getFileName() == null ? "upload.bin" : filePath.getFileName().toString();
            String contentType = resolveContentType(filePath);
            byte[] fileBytes = Files.readAllBytes(filePath);

            List<byte[]> byteArrays = new ArrayList<>();
            byteArrays.add(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            byteArrays.add(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            byteArrays.add(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            byteArrays.add(fileBytes);
            byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
            byteArrays.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            HttpRequest request = createRequestBuilder(path)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArrays(byteArrays))
                    .build();
            return send(request, dataType);
        } catch (IOException ex) {
            throw new ApiException("上传文件读取失败", ex);
        }
    }

    public byte[] download(String path, Map<String, ?> queryParams) {
        HttpRequest request = createRequestBuilder(appendQueryParams(path, queryParams))
                .GET()
                .build();
        return sendForBytes(request);
    }

    private <T> ApiResponse<T> send(HttpRequest request, Class<T> dataType) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JavaType responseType = objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, dataType);
            return objectMapper.readValue(response.body(), responseType);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiException("接口请求失败", ex);
        } catch (IOException ex) {
            throw new ApiException("接口请求失败", ex);
        }
    }

    private <T> ApiResponse<List<T>> sendList(HttpRequest request, Class<T> itemType) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, itemType);
            JavaType responseType = objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, listType);
            return objectMapper.readValue(response.body(), responseType);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiException("接口请求失败", ex);
        } catch (IOException ex) {
            throw new ApiException("接口请求失败", ex);
        }
    }

    private byte[] sendForBytes(HttpRequest request) {
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new ApiException("文件下载失败，HTTP状态码：" + statusCode);
            }
            return response.body();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiException("文件下载失败", ex);
        } catch (IOException ex) {
            throw new ApiException("文件下载失败", ex);
        }
    }

    private String toJson(Object requestBody) {
        if (requestBody == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(requestBody);
        } catch (IOException ex) {
            throw new ApiException("请求参数序列化失败", ex);
        }
    }

    private HttpRequest.Builder createRequestBuilder(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(resolveUrl(path)))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json");

        String token = appConfigService.load().getToken();
        if (token != null && !token.trim().isEmpty()) {
            builder.header("token", token.trim());
        }
        return builder;
    }

    private String resolveUrl(String path) {
        String baseUrl = appConfigService.load().getServerConfig().getBaseUrl();
        String normalizedBaseUrl = (baseUrl == null || baseUrl.trim().isEmpty()) ? "http://127.0.0.1:8080" : baseUrl.trim();
        String normalizedPath = path == null ? "" : path.trim();

        if (normalizedPath.startsWith("http://") || normalizedPath.startsWith("https://")) {
            return normalizedPath;
        }

        if (!normalizedBaseUrl.endsWith("/") && !normalizedPath.startsWith("/")) {
            return normalizedBaseUrl + "/" + normalizedPath;
        }
        if (normalizedBaseUrl.endsWith("/") && normalizedPath.startsWith("/")) {
            return normalizedBaseUrl + normalizedPath.substring(1);
        }
        return normalizedBaseUrl + normalizedPath;
    }

    private String appendQueryParams(String path, Map<String, ?> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return path;
        }

        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, ?> entry : queryParams.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key == null || key.trim().isEmpty() || value == null) {
                continue;
            }

            String text = String.valueOf(value).trim();
            if (text.isEmpty()) {
                continue;
            }

            joiner.add(encode(key.trim()) + "=" + encode(text));
        }

        String queryString = joiner.toString();
        if (queryString.isEmpty()) {
            return path;
        }

        if (path.contains("?")) {
            return path + "&" + queryString;
        }
        return path + "?" + queryString;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String resolveContentType(Path filePath) {
        try {
            String contentType = Files.probeContentType(filePath);
            if (contentType != null && !contentType.trim().isEmpty()) {
                return contentType;
            }
        } catch (IOException ignored) {
            // Fall through to default content type when probe fails.
        }
        return "application/octet-stream";
    }
}
