package ai.starwhale.mlops.domain.swds.upload;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * contains raw yaml info & uploading phase info
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VersionMeta {

    /**
     * yaml
     */
    Manifest manifest;

    /**
     * key: file name
     * value: blake2b
     */
    Map<String,String> uploadedFileBlake2bs;

}
