// 복호화 파일

package com.inspien_project.common;

import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class BaseData {

    // Base64를 AES 방식으로 복호화함.
    public static String decrypt(String encoded, byte[] key) throws Exception {
        byte[] cipherBytes = Base64.getDecoder().decode(encoded.replaceAll("\\s", ""));
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(cipher.doFinal(cipherBytes))).toString();
    }
}