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
                        .description("Stage 2.2：包含活动、报名、候补、地图、活动群聊和系统通知接口。WebSocket 端点：/ws?token=<JWT>。")
                        .version("2.2.0"));
    }
}
