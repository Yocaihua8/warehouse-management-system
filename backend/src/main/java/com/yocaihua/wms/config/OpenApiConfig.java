package com.yocaihua.wms.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 全局配置。
 * 访问地址：http://localhost:8080/swagger-ui.html
 * <p>
 * 认证方式：请求 Header 中携带 token（通过 /user/login 获取）。
 * 在 Swagger UI 右上角点击 "Authorize"，输入 token 值后，后续请求会自动携带。
 */
@Configuration
@SecurityScheme(
        name = "token",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "token",
        description = "调用 /user/login 获取 token，在此处填入后即可访问需要鉴权的接口"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("仓库管理系统 API")
                        .description("""
                                ## 仓库管理系统后端接口文档

                                **认证方式**：调用 `/user/login` 获取 token，在右上角 **Authorize** 中填入 token 值，后续请求自动携带。

                                **角色说明**：
                                - `ADMIN`：管理员，可操作全部接口（含确认/作废订单、调整库存、AI 确认入库等）
                                - `OPERATOR`：操作员，可查询、新建草稿，不可执行审批类操作

                                **状态码说明**：所有接口统一返回 `{ code, message, data }`，`code=1` 为成功，`code=0` 为业务错误。
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("WMS Team")
                        )
                );
    }
}
