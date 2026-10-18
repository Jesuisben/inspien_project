package com.inspien_project.test.order;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class OrderApiTest {

    // IntelliJ의 실행 버튼에서 시작하는 진입점이며 이 메서드가 호출하는 작업만 실행
    public static void main(String[] args) {
        JFileChooser chooser = new JFileChooser(Path.of("private").toAbsolutePath().toFile());
        chooser.setDialogTitle("주문 요청에 사용할 XML 파일 선택");
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
        try {
            Path file = chooser.getSelectedFile().toPath();
            if (Files.size(file) == 0 || Files.size(file) > 1_048_576) throw new IllegalArgumentException("XML_SIZE");
            byte[] xml = Files.readAllBytes(file);
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:9000/orders"))
                    .timeout(Duration.ofSeconds(90)).header("Content-Type", "application/xml")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(xml)).build();
            var response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
                    .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String requestId = response.headers().firstValue("X-Request-Id").orElse("없음");
            String result = "HTTP " + response.statusCode() + "\nX-Request-Id: " + requestId + "\n" + response.body();
            System.out.println(result);
            JOptionPane.showMessageDialog(null, result);
        } catch (Exception error) {
            JOptionPane.showMessageDialog(null, "요청 실패: " + error.getClass().getSimpleName()
                    + "\n서버 실행 여부, XML 경로, 파일 크기 확인");
        }
    }
}
