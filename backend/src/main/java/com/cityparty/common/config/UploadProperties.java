package com.cityparty.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "city-party.upload")
public class UploadProperties {

    private String baseDir;
    private String avatarDir;
    private String activityDir;
    private String publicPrefix;
}
