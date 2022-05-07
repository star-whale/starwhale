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
