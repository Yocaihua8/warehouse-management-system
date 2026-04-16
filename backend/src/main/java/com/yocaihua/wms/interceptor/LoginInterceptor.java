package com.yocaihua.wms.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yocaihua.wms.common.CurrentUserContext;
import com.yocaihua.wms.common.Result;
import com.yocaihua.wms.common.TokenStore;
import com.yocaihua.wms.common.UserRoleConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final TokenStore tokenStore;

    public LoginInterceptor(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        CurrentUserContext.clear();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("token");
        TokenStore.UserSession session = tokenStore.getSession(token);
        String username = session == null ? null : session.getUsername();
        String role = session == null ? null : session.getRole();
        if ((role == null || role.trim().isEmpty()) && "admin".equalsIgnoreCase(username)) {
            role = UserRoleConstant.ADMIN;
        }
        if (username != null && !username.trim().isEmpty()) {
            CurrentUserContext.setUsername(username);
            CurrentUserContext.setRole(role);
            if (!hasPermission(request, role)) {
                responseForbidden(response, "当前角色无权限访问该接口");
                return false;
            }
            return true;
        }

        responseUnauthorized(response, "未登录或登录已失效");

        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserContext.clear();
    }

    private boolean hasPermission(HttpServletRequest request, String role) {
        String normalizedRole = UserRoleConstant.normalize(role);
        if (UserRoleConstant.ADMIN.equals(normalizedRole)) {
            return true;
        }
        return !isAdminOnlyRequest(request);
    }

    private boolean isAdminOnlyRequest(HttpServletRequest request) {
        String method = request.getMethod() == null ? "" : request.getMethod().toUpperCase();
        String path = request.getRequestURI() == null ? "" : request.getRequestURI();

        if (path.startsWith("/product/add") && "POST".equals(method)) {
            return true;
        }
        if (path.startsWith("/product/update") && "PUT".equals(method)) {
            return true;
        }
        if (path.startsWith("/product/delete/") && "DELETE".equals(method)) {
            return true;
        }
        if (path.startsWith("/customer/add") && "POST".equals(method)) {
            return true;
        }
        if (path.startsWith("/customer/update") && "PUT".equals(method)) {
            return true;
        }
        if (path.startsWith("/customer/delete/") && "DELETE".equals(method)) {
            return true;
        }
        if (path.startsWith("/supplier/add") && "POST".equals(method)) {
            return true;
        }
        if (path.startsWith("/supplier/update") && "PUT".equals(method)) {
            return true;
        }
        if (path.startsWith("/supplier/delete/") && "DELETE".equals(method)) {
            return true;
        }
        if (path.startsWith("/user/add") && "POST".equals(method)) {
            return true;
        }
        if (path.startsWith("/user/update") && "PUT".equals(method)) {
            return true;
        }
        if (path.startsWith("/user/delete/") && "DELETE".equals(method)) {
            return true;
        }
        if (path.startsWith("/inbound-order/") && path.endsWith("/confirm") && "POST".equals(method)) {
            return true;
        }
        if (path.startsWith("/inbound-order/") && path.endsWith("/void") && "POST".equals(method)) {
            return true;
        }
        if (path.startsWith("/outbound-order/") && path.endsWith("/confirm") && "POST".equals(method)) {
            return true;
        }
        if (path.startsWith("/outbound-order/") && path.endsWith("/void") && "POST".equals(method)) {
            return true;
        }
        if (path.startsWith("/stock/update") && "PUT".equals(method)) {
            return true;
        }
        if (path.equals("/stock/low-alert/trigger") && "POST".equals(method)) {
            return true;
        }
        if (path.equals("/operation/log/list") && "GET".equals(method)) {
            return true;
        }
        if (path.equals("/ai/inbound/confirm") && "POST".equals(method)) {
            return true;
        }
        return path.equals("/ai/outbound/confirm") && "POST".equals(method);
    }

    private void responseUnauthorized(HttpServletResponse response, String message) throws Exception {
        writeErrorResponse(response, 401, message);
    }

    private void responseForbidden(HttpServletResponse response, String message) throws Exception {
        writeErrorResponse(response, 403, message);
    }

    private void writeErrorResponse(HttpServletResponse response, int httpStatus, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(httpStatus);

        Result<String> result = Result.error(message);
        String json = new ObjectMapper().writeValueAsString(result);

        PrintWriter writer = response.getWriter();
        writer.write(json);
        writer.flush();
        writer.close();
    }
}
