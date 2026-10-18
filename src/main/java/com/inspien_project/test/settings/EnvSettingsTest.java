// 환경변수 출력하는 테스트

package com.inspien_project.test.settings;

import com.inspien_project.common.EnvSettings;

public class EnvSettingsTest {

    // 실제 .env를 읽고 PPT 요청 항목 다음에 API 인증 항목을 순서대로 출력함.
    public static void main(String[] args) {
        EnvSettings envSettings = EnvSettings.load();

        printSetting("NAME", envSettings.getName());
        printSetting("PHONE_NUMBER", envSettings.getPhoneNumber());
        printSetting("E_MAIL", envSettings.getEmail());
        printSetting("baseUsername", envSettings.getBaseUsername());
        printSetting("basePassword", envSettings.getBasePassword());

        System.out.println("\n환경변수 값 출력 완료");
    }

    private static void printSetting(String key, String value) {
        String display;
        if (value == null || value.isBlank()) {
            display = "환경변수 값 없음";
        } else {
            display = value;
        }
        System.out.println(key + "=" + display);
    }
}