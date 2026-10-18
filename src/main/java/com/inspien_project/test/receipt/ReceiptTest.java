package com.inspien_project.test.receipt;

import static com.inspien_project.test.common.OrderTestHelper.*;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import com.inspien_project.common.EnvSettings;
import com.inspien_project.dto.OrderInputDto;
import com.inspien_project.dto.OrderDataDto;
import com.inspien_project.service.ReceiptService;

public class ReceiptTest {

    // main에서 직접 호출하는 검증 사례임. 예상 결과가 다르면 예외로 중단
    Path folder;

    // 영수증 이름·내용을 확인하고 같은 초의 파일을 덮어쓰지 않는지 검사
    void testReceiptFile() throws Exception {
        var settings = new EnvSettings();
        settings.setApplicantKey("KEY");
        settings.setName("테스트");
        settings.setReceiptDirectory(folder.toString());
        settings.setReceiptCharset("EUC-KR");
        var clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        var receiptService = new ReceiptService(settings, clock);
        var orderData = OrderDataDto.createOrderData("A000", "KEY", new OrderInputDto("U", "I", "홍길동", "서울", "상품", "0010"));
        var receipt = receiptService.createReceipt(List.of(orderData));
        assertEquals("INSPIEN_테스트_20260101090000.txt", receipt.getName());
        byte[] originalBytes = Files.readAllBytes(receipt.getPath());
        assertEquals("A000^U^I^KEY^홍길동^서울^상품^0010\n", new String(originalBytes, Charset.forName("EUC-KR")));
        assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> receiptService.createReceipt(List.of(orderData)));
        assertArrayEquals(originalBytes, Files.readAllBytes(receipt.getPath()));
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("target/checks"));
        {
            var test = new ReceiptTest();
            test.folder = Files.createTempDirectory(Path.of("target/checks"), "ReceiptTest-");
            test.testReceiptFile();
        }
        System.out.println("ReceiptTest 확인 완료");
    }
}
