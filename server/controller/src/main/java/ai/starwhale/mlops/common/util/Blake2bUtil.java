/**
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

package ai.starwhale.mlops.common.util;

import com.rfksystems.blake2b.Blake2b;
import org.apache.commons.codec.binary.Hex;

/**
 * Blake2b digest
 */
public class Blake2bUtil {

    public static String digest(byte[] bytes){
        final Blake2b digest = new Blake2b();
        digest.update(bytes,0,bytes.length);
        final byte[] out = new byte[64];
        digest.digest(out, 0);
        return new String(Hex.encodeHex(out));
    }

}
