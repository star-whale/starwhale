package ai.starwhale.mlops.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageObjectInfo {
    boolean exists;
    Long contentLength;
    String metaInfo;
}
