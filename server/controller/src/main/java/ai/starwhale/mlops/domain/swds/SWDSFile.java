/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds;

import java.io.File;

public class SWDSFile {

    private final String projectId;
    private final String swdsId;

    public SWDSFile(String projectId, String swdsId) {
        this.projectId = projectId;
        this.swdsId = swdsId;
    }

    public String getStoragePath() {
        //todo(dreamlandliu) get storage path
        return File.separator;
    }

    public String getZipFilePath() {
        //todo(dreamlandliu) get zip file upload path
        return File.separator;
    }

    public String generateZipFileName() {
        //todo(dreamlandliu) generate temp file name
        return swdsId;
    }

    public String meta() {
        //todo(dreamlandliu) meta.json
        return "{}";
    }
}
