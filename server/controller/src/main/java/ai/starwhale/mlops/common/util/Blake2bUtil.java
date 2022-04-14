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
