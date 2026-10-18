
package com.inspien_project.test.basedata;

import static com.inspien_project.test.common.OrderTestHelper.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import com.inspien_project.common.BaseData;

public class BaseDataTest {

    void testDecrypt() throws Exception {
        byte[] key = Arrays.copyOf(MessageDigest.getInstance("SHA-1")
                .digest("010-0000-0000".getBytes(StandardCharsets.UTF_8)), 16);
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
        String encoded = Base64.getEncoder().encodeToString(cipher.doFinal("테스트 연결 정보".getBytes(StandardCharsets.UTF_8)));
        assertEquals("테스트 연결 정보", BaseData.decrypt(encoded, key));
        byte[] wrongKey = new byte[16];
        assertThrows(Exception.class, () -> BaseData.decrypt(encoded, wrongKey));
    }

    public static void main(String[] args) throws Exception {
        {
            var test = new BaseDataTest();
            test.testDecrypt();
        }
        System.out.println("BaseDataTest 확인 완료");
    }
}