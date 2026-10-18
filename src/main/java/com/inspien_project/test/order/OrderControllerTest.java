package com.inspien_project.test.order;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import static com.inspien_project.test.common.OrderTestHelper.*;

public class OrderControllerTest {

    // 먼저 실제 앱을 실행함. 아래 세 요청은 유효한 주문이 아니므로 회사 DB·FTP에 쓰면 안 됨
    public static void main(String[] args) throws Exception {
        checkRequest("text/plain", "bad", 415);
        checkRequest("application/xml", "", 400);
        checkRequest("application/xml", "<broken>", 400);
        System.out.println("OrderControllerTest 확인 완료");
    }

    private static void checkRequest(String contentType, String body, int expectedStatus) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:9000/orders"))
                .timeout(Duration.ofSeconds(15)).header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        var response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()
                .send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(expectedStatus, response.statusCode());
        assertTrue(response.headers().firstValue("X-Request-Id").filter(value -> !value.isBlank()).isPresent());
        System.out.println("HTTP " + response.statusCode() + " / 요청 ID 확인");
    }
}
