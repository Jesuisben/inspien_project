
package com.inspien_project.common;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JOptionPane;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeType;

public class BaseData {

    private static final JsonMapper json = JsonMapper.builder().build();
    private static final URI endpoint = URI.create(
            "https://inspien.it-cpi002-rt.cfapps.ap10.hana.ondemand.com/http/RecruitingTest"
    );

    public static void main(String[] args) {
        try {
            EnvSettings settings = EnvSettings.load();
            String name = required(settings.getName(), "NAME");
            String phoneNumber = required(settings.getPhoneNumber(), "PHONE_NUMBER");
            String email = required(settings.getEmail(), "E_MAIL");
            String baseUsername = required(settings.getBaseUsername(), "baseUsername");
            String basePassword = required(settings.getBasePassword(), "basePassword");
            if (!phoneNumber.matches("010-[0-9]{4}-[0-9]{4}")) {
                throw new IllegalArgumentException("PHONE_NUMBER_FORMAT");
            }
            byte[] body = json.writeValueAsBytes(Map.of("NAME", name,
                    "PHONE_NUMBER", phoneNumber, "E_MAIL", email));

            String authorization = Base64.getEncoder().encodeToString(
                    (baseUsername + ":" + basePassword).getBytes(StandardCharsets.UTF_8));
            var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + authorization)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
            var response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
                    .send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                JOptionPane.showMessageDialog(null, "HTTP " + response.statusCode()
                        + "입니다. 입력과 PPT의 API 인증정보 다시 확인");
                return;
            }
            JsonNode root = json.readTree(response.body());
            if (!root.isObject() || !root.hasNonNull("APPLICANT_KEY") || !root.hasNonNull("SAMPLE_DATA")) {
                throw new IllegalArgumentException("RESPONSE_STRUCTURE");
            }

            byte[] key = Arrays.copyOf(MessageDigest.getInstance("SHA-1")
                    .digest(phoneNumber.getBytes(StandardCharsets.UTF_8)), 16);
            var connections = json.createObjectNode();
            connections.set("APPLICANT_KEY", root.get("APPLICANT_KEY"));
            int[] decoded = {0};

            for (String field : new String[]{"ORDER_TB_CONN", "SHIPMENT_TB_CONN", "FTP_CONN"}) {
                if (!root.hasNonNull(field)) throw new IllegalArgumentException("CONNECTION_FIELD");
                connections.set(field, decryptJson(root.get(field), key, decoded));
            }
            Path folder = Path.of("private", "received-" + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))).toAbsolutePath();
            Files.createDirectories(folder.getParent());

            Files.createDirectory(folder);
            Files.write(folder.resolve("Response.json"), response.body(), StandardOpenOption.CREATE_NEW);
            Files.writeString(folder.resolve("ConnectionInfo.json"),
                    json.writerWithDefaultPrettyPrinter().writeValueAsString(connections), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);

            Files.write(folder.resolve("Sample.xml"), Base64.getDecoder().decode(
                    root.get("SAMPLE_DATA").asString().replaceAll("\\s", "")), StandardOpenOption.CREATE_NEW);
            JOptionPane.showMessageDialog(null, "저장 위치: " + folder + "\n복호화한 값 개수: " + decoded[0]
                    + "\nConnectionInfo.json, Sample.xml 확인");
        } catch (Exception error) {         
            JOptionPane.showMessageDialog(null, "요청 및 파일 저장 실패: " + error.getClass().getSimpleName()
                    + "\n네트워크 및 응답 확인");
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank() || value.contains("입력해야함")) {
            throw new IllegalArgumentException("설정 확인: " + name);
        }
        return value;
    }

    public static String decrypt(String encoded, byte[] key) throws Exception {
        byte[] cipherBytes = Base64.getDecoder().decode(encoded.replaceAll("\\s", ""));
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(cipher.doFinal(cipherBytes))).toString();
    }

    private static JsonNode decryptJson(JsonNode node, byte[] key, int[] decoded) {
        if (node.isObject()) {
            var result = json.createObjectNode();
            for (var entry : node.properties()) result.set(entry.getKey(), decryptJson(entry.getValue(), key, decoded));
            return result;
        }
        if (node.isArray()) {
            var result = json.createArrayNode();
            for (var value : node) result.add(decryptJson(value, key, decoded));
            return result;
        }
        if (node.getNodeType() == JsonNodeType.STRING) {
            try {
                String plain = decrypt(node.asString(), key);
                decoded[0]++;
                try { return json.readTree(plain); }
                catch (Exception ignored) { return json.getNodeFactory().textNode(plain); }
            } catch (Exception ignored) {
                return node;
            }
        }
        return node;
    }
}