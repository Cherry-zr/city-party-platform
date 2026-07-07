package com.cityparty.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI cityPartyOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("同城活动发现与陌生人组局平台 API")
                        .description("第一阶段核心接口文档")
                        .version("1.0.0"));
    }
}
