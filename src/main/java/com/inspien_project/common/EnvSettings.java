// 환경변수 공통 파일

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

    // 회사 응답의 지원자 키와 주문 DB 접속 정보
    private String applicantKey;
    private String orderDbUrl;
    private String orderDbUsername;
    private String orderDbPassword;

    // 회사 DB와 분리한 로컬 MySQL 검증 계정
    private String testDbUsername;
    private String testDbPassword;

    // 영수증 파일의 저장 위치와 내용의 문자 인코딩
    private String receiptDirectory = "private/receipts";
    private String receiptCharset = "EUC-KR";

    // 회사 제공 FTP 접속 정보이며 실제 값은 .env에서 읽음
    private String ftpHost;
    private int ftpPort = 21;
    private String ftpUsername;
    private String ftpPassword;
    private String ftpDirectory = "/";
    private String ftpControlEncoding = "UTF-8";

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

    // DB 연결 생성 함수에 값을 함께 전달하기 위한 묶음
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