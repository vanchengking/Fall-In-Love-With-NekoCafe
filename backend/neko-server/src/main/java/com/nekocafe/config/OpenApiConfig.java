package com.nekocafe.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 文档配置，含 Bearer JWT 鉴权方案。
 * 访问地址：/swagger-ui.html ，原始文档：/v3/api-docs 。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI nekoOpenAPI() {
        final String scheme = "bearer-jwt";
        return new OpenAPI()
                .info(new Info()
                        .title("NekoCafe 智慧餐饮预约平台 API")
                        .description("门店 / 桌位 / 预约状态机 / 点单支付沙箱 / 猫咪档案 / 运营看板 REST 接口（D-01）")
                        .version("0.1.0"))
                .addSecurityItem(new SecurityRequirement().addList(scheme))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
