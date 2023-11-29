/*
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.starwhale.mlops.storage.fs;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "sw.storage", name = "type", havingValue = "fs")
public class FsStorageSignature {

    private static final String hmacSHA256 = "HmacSHA256";

    final String privateKey;

    public FsStorageSignature(@Value("${sw.storage.fs-config.sig-key}") String privateKey) {
        this.privateKey = privateKey;
    }

    public String sign(String path, Long expMilli) {
        return generateHmacSignature(path + expMilli, privateKey);
    }

    public boolean valid(String path, Long expMilli, String sign) {
        return sign.equals(generateHmacSignature(path + expMilli, privateKey));
    }

    private static String generateHmacSignature(String data, String key) {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), hmacSHA256);
        Mac mac;
        try {
            mac = Mac.getInstance(hmacSHA256);
            mac.init(secretKeySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        byte[] hash = mac.doFinal(data.getBytes());

        // Convert byte array to hexadecimal string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }
}
