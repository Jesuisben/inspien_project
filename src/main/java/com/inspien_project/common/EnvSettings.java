
package com.inspien_project.common;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties
@Getter
@Setter
public class EnvSettings {
    private String name;
    private String phoneNumber;
    private String email;
    private String baseUsername;
    private String basePassword;

    private String applicantKey;
    private String orderDbUrl;
    private String orderDbUsername;
    private String orderDbPassword;

    private String testDbUsername;
    private String testDbPassword;

    private String receiptDirectory = "private/receipts";
    private String receiptCharset = "EUC-KR";

    private String ftpHost;
    private int ftpPort = 21;
    private String ftpUsername;
    private String ftpPassword;
    private String ftpDirectory = "/";
    private String ftpControlEncoding = "UTF-8";
    private String shipmentDbUrl;
    private String shipmentDbUsername;
    private String shipmentDbPassword;
    private boolean batchEnabled;
    private String batchCron = "0 */5 * * * *";

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EnvSettings.class)
    static class SettingsConfig { }

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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Target {
        private String url;
        private String username;
        private String password;
    }
}