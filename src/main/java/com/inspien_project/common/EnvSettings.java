// 환경변수 공통 파일

package com.inspien_project.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// application.properties의 환경변수 자동으로 이름이 같은 맴버 변수에 할당
@ConfigurationProperties
@Getter
@Setter
public class EnvSettings {
    private String name;
    private String phoneNumber;
    private String email;
    private String baseUsername;
    private String basePassword;

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EnvSettings.class)
    static class SettingsConfig { }

    // 환경변수 재사용 함수
    public static EnvSettings load() {
        SpringApplication application = new SpringApplication(SettingsConfig.class);
        application.setHeadless(false);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);
        try (var context = application.run()) {
            return context.getBean(EnvSettings.class);
        }
    }
}