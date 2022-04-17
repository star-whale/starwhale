package ai.starwhale.mlops.domain.swds.upload;

import ai.starwhale.mlops.domain.swds.SWDatasetVersionEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SWDSVersionWithMeta {

    SWDatasetVersionEntity swDatasetVersionEntity;
    VersionMeta versionMeta;

}
