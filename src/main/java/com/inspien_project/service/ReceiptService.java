package com.inspien_project.service;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import com.inspien_project.common.EnvSettings;
import com.inspien_project.dto.OrderDataDto;

@Service
public class ReceiptService {

    private final EnvSettings envSettings;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public ReceiptService(EnvSettings envSettings) {
        this(envSettings, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    public ReceiptService(EnvSettings envSettings, Clock clock) {
        if (envSettings.getReceiptDirectory() == null || envSettings.getReceiptDirectory().isBlank()
                || envSettings.getReceiptDirectory().contains("입력필요")) {
            throw new IllegalArgumentException("영수증 폴더 설정을 확인");
        }
        Charset.forName(envSettings.getReceiptCharset());
        this.envSettings = envSettings;
        this.clock = clock;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiptFile {
        private String name;
        private Path path;
    }

    // 파일명 규칙과 초 단위 충돌 검사를 적용하여 로컬 영수증 파일을 준비
    public ReceiptFile createReceipt(List<OrderDataDto> orderList) throws IOException {
        String time = LocalDateTime.now(clock)
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String name = "INSPIEN_" + envSettings.getName() + "_" + time + ".txt";
        Path folder = Path.of(envSettings.getReceiptDirectory()).toAbsolutePath();
        Files.createDirectories(folder);
        Path path = folder.resolve(name);
        // 초 단위 파일명이 충돌하면 실패시켜 기존 영수증 덮어쓰기를 막음
        Files.write(path, toReceiptBytes(orderList, Charset.forName(envSettings.getReceiptCharset())),
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return new ReceiptFile(name, path);
    }

    // PPT의 8개 필드 순서와 LF를 지키며 표현 불가능한 문자를 물음표로 바꾸지 않고 실패시킴
    public static byte[] toReceiptBytes(List<OrderDataDto> orderList, Charset charset) throws IOException {
        StringBuilder text = new StringBuilder();
        for (OrderDataDto orderData : orderList) {
            text.append(String.join("^", orderData.getOrderId(), orderData.getUserId(), orderData.getItemId(),
                    orderData.getApplicantKey(), orderData.getName(), orderData.getAddress(), orderData.getItemName(), orderData.getPrice()))
                    .append('\n');
        }
        ByteBuffer buffer = charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(text));
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }
}
